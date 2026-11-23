package dad.season;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Persistencia del estado de temporada de cada servidor y del histórico de CSV. Hay una implementación con
 * Firestore ({@link FirestoreSeasonStore}) y otra en memoria en los tests.
 *
 * Las operaciones marcadas como "atómicas" deben hacerse en una transacción: son las que garantizan que
 * la transición ocurre una sola vez aunque haya reinicios o dos comprobaciones a la vez.
 */
public interface SeasonStore {

    enum Status { ACTIVE, CLOSING }

    /** Estado guardado. {@code closingSeason} solo tiene sentido en {@link Status#CLOSING}. */
    record Stored(long currentSeason, Status status, long closingSeason) {
    }

    Optional<Stored> read(String guildId) throws Exception;

    /** Atómica: crea el estado con {@code season} ACTIVE si todavía no existe. Si existe, no hace nada. */
    void initializeIfAbsent(String guildId, long season) throws Exception;

    /**
     * Atómica: si el estado es ACTIVE y {@code currentSeason == season}:
     * <ol>
     *   <li>lee a todos los jugadores y guarda el CSV de la temporada en el histórico (si aún no existe;
     *       se omite solo en la pretemporada sin jugadores),</li>
     *   <li>pasa el estado a CLOSING(season),</li>
     * </ol>
     * todo en la misma transacción, y devuelve true. En cualquier otro caso no toca nada y devuelve false.
     * Así el CSV queda guardado en la base de datos ANTES de que se reinicie ningún jugador.
     */
    boolean beginClosing(String guildId, long season) throws Exception;

    /** Lee todos los jugadores del servidor. */
    List<SeasonCsv.Row> readPlayers(String guildId) throws Exception;

    /** Reinicia las estadísticas de temporada de todos los jugadores. Debe ser idempotente. */
    void resetPlayers(String guildId) throws Exception;

    /** Atómica: si está en CLOSING(closedSeason), pasa a ACTIVE con {@code newSeason}. */
    void finishClosing(String guildId, long closedSeason, long newSeason) throws Exception;

    // ==================== HISTÓRICO DE CSV ====================

    /** CSV guardado de una temporada cerrada. */
    Optional<String> readArchivedCsv(String guildId, long season) throws Exception;

    /** Atómica: guarda el CSV de una temporada solo si no había ninguno (nunca sobrescribe). */
    void saveArchivedCsvIfAbsent(String guildId, long season, String csv) throws Exception;

    /** Última temporada con CSV guardado. */
    OptionalLong latestArchivedSeason(String guildId) throws Exception;

    /** Todas las temporadas con CSV guardado. */
    List<Long> listArchivedSeasons(String guildId) throws Exception;
}
