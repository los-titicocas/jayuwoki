package dad.season;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Transición de temporada: exporta el CSV de la que termina, reinicia a los jugadores y abre la siguiente.
 *
 * <h2>Por qué ocurre una sola vez (aunque el bot se reinicie)</h2>
 * El nº de temporada en curso se guarda en Firestore ({@code {guildId}/Season}) y se compara con el que
 * corresponde a la fecha de hoy. El cierre tiene 3 pasos:
 * <ol>
 *   <li>{@code beginClosing}: UNA transacción que guarda el CSV de la temporada n en Firestore
 *       ({@code {guildId}/Season/Archive/{n}}) y pasa ACTIVE(n) → CLOSING(n). Solo un proceso/hilo puede ganarla.</li>
 *   <li>Se copia el CSV al disco local (caché) y DESPUÉS se reinician los jugadores. El reinicio es idempotente.</li>
 *   <li>{@code finishClosing}: CLOSING(n) → ACTIVE(temporada de hoy).</li>
 * </ol>
 * Si el bot se cae en mitad, al arrancar ve CLOSING y retoma desde el paso 2. Como el CSV ya está en Firestore
 * antes del reinicio y nunca se sobrescribe, no se pierde ni se exporta una tabla ya reiniciada, aunque el bot
 * vuelva a arrancar en otra máquina.
 *
 * <h2>Persistencia del CSV</h2>
 * La copia buena es la de Firestore. Los ficheros de {@link SeasonArchive} son solo una caché local: si se
 * borran o se cambia de equipo, {@link #findCsv} los vuelve a descargar.
 *
 * Si el bot estuvo apagado varias temporadas, se cierra la última que tenía datos y se salta directamente a
 * la actual (las intermedias no tuvieron partidas, así que no generan CSV).
 */
public class SeasonManager {

    /** Aviso tras cerrar una temporada (limpiar privaditas, anunciar en Discord…). */
    public interface Listener {
        void onSeasonClosed(String guildId, long closedSeason, long newSeason, Optional<Path> csv);
    }

    /** Resultado de una transición hecha por este proceso. */
    public record Transition(long closedSeason, long newSeason, Optional<Path> csv) {
    }

    private final SeasonStore store;
    private final SeasonArchive archive;
    private final Supplier<LocalDate> today;
    private volatile Listener listener = (g, c, n, f) -> { };

    /** Temporada ACTIVE ya confirmada en Firestore, para no consultarla en cada comprobación. */
    private final Map<String, Long> confirmedSeason = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public SeasonManager(SeasonStore store, SeasonArchive archive, Supplier<LocalDate> today) {
        this.store = store;
        this.archive = archive;
        this.today = today;
    }

    public void setListener(Listener listener) {
        this.listener = listener != null ? listener : (g, c, n, f) -> { };
    }

    public SeasonArchive getArchive() {
        return archive;
    }

    public long currentSeasonByDate() {
        return SeasonCalendar.seasonAt(today.get());
    }

    /**
     * Ejecuta {@code action} con el lock del servidor. Lo usan los comandos que leen/escriben ELO
     * ($privadita, $resultadoPrivadita) para no cruzarse nunca con un reinicio de temporada.
     */
    public void runLocked(String guildId, Runnable action) {
        ReentrantLock lock = locks.computeIfAbsent(guildId, k -> new ReentrantLock());
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Comprueba la fecha y, si ha terminado la temporada guardada, hace la transición.
     * Barato cuando no hay nada que hacer (no toca Firestore si ya está confirmada la temporada de hoy).
     *
     * @return la transición realizada, o vacío si no tocaba (o la hizo otro proceso).
     */
    public Optional<Transition> ensureCurrent(String guildId) throws Exception {
        long target = SeasonCalendar.seasonAt(today.get());
        Long known = confirmedSeason.get(guildId);
        if (known != null && known >= target) {
            return Optional.empty();
        }

        ReentrantLock lock = locks.computeIfAbsent(guildId, k -> new ReentrantLock());
        lock.lock();
        try {
            Optional<Transition> result = Optional.empty();
            for (int attempt = 0; attempt < 5; attempt++) {
                Optional<SeasonStore.Stored> stored = store.read(guildId);
                if (stored.isEmpty()) {
                    // Primera vez para este servidor: los datos que ya había son la pretemporada (T0).
                    // ⚠️ DECISIÓN: al empezar la Temporada 1 (23/11/2026) esos datos se exportan a
                    // temporada-00_pretemporada…csv y se reinician, para que la T1 empiece de cero.
                    store.initializeIfAbsent(guildId, SeasonCalendar.PRESEASON);
                    continue;
                }
                SeasonStore.Stored st = stored.get();

                if (st.status() == SeasonStore.Status.CLOSING) {
                    System.out.println("♻️ [Temporadas] Retomando cierre interrumpido de la temporada "
                            + st.closingSeason() + " en " + guildId);
                    result = Optional.of(completeClosing(guildId, st.closingSeason(), Math.max(target, st.closingSeason() + 1)));
                    continue; // releer: por si hace falta otra transición
                }
                if (st.currentSeason() >= target) {
                    confirmedSeason.put(guildId, st.currentSeason());
                    return result;
                }
                if (store.beginClosing(guildId, st.currentSeason())) {
                    result = Optional.of(completeClosing(guildId, st.currentSeason(), target));
                }
                // si beginClosing devolvió false, otro proceso se adelantó: releer
            }
            throw new IllegalStateException("No se pudo estabilizar el estado de temporada de " + guildId);
        } finally {
            lock.unlock();
        }
    }

    private Transition completeClosing(String guildId, long closing, long next) throws Exception {
        Optional<Path> csv = archiveBeforeReset(guildId, closing);
        store.resetPlayers(guildId);
        store.finishClosing(guildId, closing, next);
        confirmedSeason.put(guildId, next);
        System.out.println("✅ [Temporadas] " + guildId + ": temporada " + closing + " cerrada, empieza la " + next);

        try {
            listener.onSeasonClosed(guildId, closing, next, csv);
        } catch (RuntimeException e) {
            e.printStackTrace(); // un fallo al anunciar no debe deshacer ni repetir la transición
        }
        return new Transition(closing, next, csv);
    }

    /**
     * Garantiza que el CSV está en Firestore (y en la caché local) antes del reinicio.
     * Normalmente ya lo guardó beginClosing; los otros casos solo se dan con cierres empezados por una versión
     * anterior del bot (que guardaba el CSV solo en disco).
     */
    private Optional<Path> archiveBeforeReset(String guildId, long closing) throws Exception {
        Optional<String> stored = store.readArchivedCsv(guildId, closing);
        if (stored.isPresent()) {
            try {
                return Optional.of(cacheLocally(guildId, closing, stored.get()));
            } catch (Exception e) {
                // El CSV ya está a salvo en Firestore: un fallo del disco local no debe bloquear el cierre
                System.err.println("⚠️ [Temporadas] No se pudo guardar la copia local del CSV: " + e);
                return Optional.empty();
            }
        }
        Optional<Path> local = archive.find(guildId, closing);
        if (local.isPresent()) {
            // Subir el CSV que solo estaba en disco: nunca se regenera con datos que podrían estar ya reiniciados
            store.saveArchivedCsvIfAbsent(guildId, closing, archive.readCsv(local.get()));
            System.out.println("☁️ [Temporadas] " + guildId + ": CSV local de la temporada " + closing + " subido a Firestore");
            return local;
        }
        List<SeasonCsv.Row> rows = store.readPlayers(guildId);
        if (rows.isEmpty() && closing == SeasonCalendar.PRESEASON) {
            return Optional.empty(); // pretemporada sin jugadores: no hay nada que archivar
        }
        String csv = SeasonCsv.build(rows);
        store.saveArchivedCsvIfAbsent(guildId, closing, csv);
        return Optional.of(cacheLocally(guildId, closing, store.readArchivedCsv(guildId, closing).orElse(csv)));
    }

    private Path cacheLocally(String guildId, long season, String csv) throws Exception {
        Optional<Path> existing = archive.find(guildId, season);
        if (existing.isPresent()) {
            return existing.get();
        }
        Path p = archive.write(guildId, season, csv);
        System.out.println("📦 [Temporadas] " + guildId + ": CSV de la temporada " + season + " guardado en Firestore y en " + p);
        return p;
    }

    /**
     * CSV de una temporada cerrada para enviarlo por Discord: usa la caché local y, si no está
     * (se borró, o el bot corre en otro equipo), lo descarga de Firestore.
     */
    public Optional<Path> findCsv(String guildId, long season) throws Exception {
        Optional<Path> local = archive.find(guildId, season);
        if (local.isPresent()) {
            return local;
        }
        Optional<String> stored = store.readArchivedCsv(guildId, season);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(archive.write(guildId, season, stored.get()));
    }

    /** Última temporada con CSV (en Firestore o, por compatibilidad, solo en disco). */
    public OptionalLong latestArchivedSeason(String guildId) throws Exception {
        OptionalLong remote = store.latestArchivedSeason(guildId);
        OptionalLong local = archive.latestSeason(guildId);
        if (remote.isEmpty()) {
            return local;
        }
        if (local.isEmpty()) {
            return remote;
        }
        return OptionalLong.of(Math.max(remote.getAsLong(), local.getAsLong()));
    }

    /** Contenido del CSV de una temporada cerrada (caché local o Firestore). */
    public Optional<String> readCsv(String guildId, long season) throws Exception {
        Optional<Path> file = findCsv(guildId, season);
        return file.isPresent() ? Optional.of(archive.readCsv(file.get())) : Optional.empty();
    }

    /** Temporadas cerradas con clasificación guardada, de la más reciente a la más antigua. */
    public List<Long> archivedSeasons(String guildId) throws Exception {
        java.util.TreeSet<Long> all = new java.util.TreeSet<>(java.util.Comparator.reverseOrder());
        all.addAll(store.listArchivedSeasons(guildId));
        all.addAll(archive.listSeasons(guildId));
        return new java.util.ArrayList<>(all);
    }
}
