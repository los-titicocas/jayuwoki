package dad.season;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el reinicio de temporada sin Firebase ni Discord: el mismo SeasonManager que usa el bot,
 * con un almacenamiento en memoria y un "reloj" que movemos a mano.
 */
class SeasonManagerTest {

    private static final String G = "123456789";

    private InMemorySeasonStore store;
    private SeasonArchive archive;
    private Path dir;
    private final AtomicReference<LocalDate> today = new AtomicReference<>();
    private final List<String> avisos = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() throws Exception {
        dir = Files.createTempDirectory("temporadas-test");
        store = new InMemorySeasonStore();
        archive = new SeasonArchive(dir);
        store.putPlayer(G, "ana", 1120, 7, 3);
        store.putPlayer(G, "luis", 980, 3, 7);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Collections.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    /** Un SeasonManager nuevo = un arranque nuevo del bot (sin caché en memoria). */
    private SeasonManager arrancarBot() {
        SeasonManager m = new SeasonManager(store, archive, today::get);
        m.setListener((g, cerrada, nueva, csv) -> avisos.add(cerrada + "->" + nueva));
        return m;
    }

    private String csv(long season) throws Exception {
        return new String(Files.readAllBytes(archive.find(G, season).orElseThrow()), StandardCharsets.UTF_8);
    }

    @Test
    void antesDel23DeNoviembreNoSeTocaNada() throws Exception {
        today.set(LocalDate.of(2026, 11, 22));
        assertTrue(arrancarBot().ensureCurrent(G).isEmpty());
        assertEquals(0, store.resetCalls);
        assertEquals(1120, store.player(G, "ana").elo());
        assertTrue(archive.latestSeason(G).isEmpty());
    }

    @Test
    void elPrimerDiaSeExportaLaPretemporadaYSeEmpiezaDeCero() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        Optional<SeasonManager.Transition> t = arrancarBot().ensureCurrent(G);

        assertTrue(t.isPresent());
        assertEquals(0, t.get().closedSeason());
        assertEquals(1, t.get().newSeason());
        String contenido = csv(0);
        assertTrue(contenido.contains("1,ana,1120,7,3,10,70.00"), contenido);
        assertTrue(contenido.contains("2,luis,980,3,7,10,30.00"), contenido);
        assertEquals(new SeasonCsv.Row("ana", 1000, 0, 0), store.player(G, "ana"));
        assertEquals(1, store.states.get(G).currentSeason());
    }

    @Test
    void laTransicionOcurreUnaSolaVezAunqueSeCompruebeMuchasVecesYSeReinicieElBot() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        SeasonManager bot = arrancarBot();
        bot.ensureCurrent(G);
        for (int i = 0; i < 10; i++) {
            assertTrue(bot.ensureCurrent(G).isEmpty());
        }
        // Se juegan partidas en la T1 y el bot se reinicia varias veces durante la temporada
        store.putPlayer(G, "ana", 1032, 1, 0);
        for (LocalDate d : List.of(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 15), LocalDate.of(2027, 2, 22))) {
            today.set(d);
            assertTrue(arrancarBot().ensureCurrent(G).isEmpty(), "no debe reiniciar el " + d);
        }
        assertEquals(1, store.resetCalls);
        assertEquals(1032, store.player(G, "ana").elo());
        assertEquals(List.of("0->1"), avisos);
    }

    @Test
    void alAcabarLaTemporada1SeGuardaSuCsvYSeReinicia() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        arrancarBot().ensureCurrent(G);
        store.putPlayer(G, "ana", 1064, 2, 0);
        store.putPlayer(G, "luis", 968, 0, 2);

        today.set(LocalDate.of(2027, 2, 23)); // primer día de la T2
        Optional<SeasonManager.Transition> t = arrancarBot().ensureCurrent(G);

        assertEquals(1, t.orElseThrow().closedSeason());
        assertEquals(2, t.get().newSeason());
        assertEquals("temporada-01_2026-11-23_a_2027-02-22.csv", archive.find(G, 1).orElseThrow().getFileName().toString());
        assertTrue(csv(1).contains("1,ana,1064,2,0,2,100.00"));
        assertEquals(new SeasonCsv.Row("luis", 1000, 0, 0), store.player(G, "luis"));
        assertEquals(1, archive.latestSeason(G).getAsLong());
    }

    @Test
    void siElBotSeCaeAMitadDelReinicioSeRetomaSinPerderNiSobrescribirElCsv() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        arrancarBot().ensureCurrent(G);
        store.putPlayer(G, "ana", 1064, 2, 0);
        store.putPlayer(G, "luis", 968, 0, 2);
        today.set(LocalDate.of(2027, 2, 23));

        store.failNextResets = 1;
        assertThrows(RuntimeException.class, () -> arrancarBot().ensureCurrent(G));
        assertEquals(SeasonStore.Status.CLOSING, store.states.get(G).status());
        assertEquals(1000, store.player(G, "ana").elo(), "ana ya se reinició");
        assertEquals(968, store.player(G, "luis").elo(), "luis todavía no");
        String csvAntes = csv(1);

        // Reinicio del bot: debe terminar el cierre y el CSV debe seguir siendo el de antes del reinicio
        Optional<SeasonManager.Transition> t = arrancarBot().ensureCurrent(G);
        assertEquals(1, t.orElseThrow().closedSeason());
        assertEquals(csvAntes, csv(1));
        assertTrue(csv(1).contains("ana,1064"));
        assertEquals(1000, store.player(G, "luis").elo());
        assertEquals(SeasonStore.Status.ACTIVE, store.states.get(G).status());
        assertEquals(2, store.states.get(G).currentSeason());
    }

    @Test
    void siElBotEstuvoApagadoVariasTemporadasSaltaALaActual() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        arrancarBot().ensureCurrent(G);
        store.putPlayer(G, "ana", 1016, 1, 0);

        today.set(LocalDate.of(2027, 9, 1)); // T4 (23 ago – 22 nov 2027)
        Optional<SeasonManager.Transition> t = arrancarBot().ensureCurrent(G);
        assertEquals(1, t.orElseThrow().closedSeason());
        assertEquals(4, t.get().newSeason());
        assertTrue(csv(1).contains("ana,1016"));
        assertEquals(4, store.states.get(G).currentSeason());
        assertEquals(2, store.resetCalls);
    }

    @Test
    void comprobacionesSimultaneasSoloReinicianUnaVez() throws Exception {
        // Caso real: el planificador y varios comandos ($privadita, $resultadoPrivadita) a la vez
        today.set(LocalDate.of(2026, 11, 23));
        SeasonManager bot = arrancarBot();
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());
        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            SeasonManager m = bot;
            Thread th = new Thread(() -> {
                try {
                    start.await();
                    m.ensureCurrent(G);
                } catch (Throwable e) {
                    errores.add(e);
                }
            });
            th.start();
            hilos.add(th);
        }
        start.countDown();
        for (Thread th : hilos) {
            th.join();
        }
        assertTrue(errores.isEmpty(), errores.toString());
        assertEquals(1, store.resetCalls);
        assertEquals(List.of("0->1"), avisos);
    }

    @Test
    void servidorNuevoSinJugadoresNoGeneraCsvDePretemporada() throws Exception {
        String nuevo = "999";
        today.set(LocalDate.of(2027, 3, 1));
        arrancarBot().ensureCurrent(nuevo);
        assertEquals(2, store.states.get(nuevo).currentSeason());
        assertTrue(archive.latestSeason(nuevo).isEmpty());
    }

    // ==================== PERSISTENCIA DEL CSV EN FIRESTORE ====================

    @Test
    void elCsvQuedaGuardadoEnLaBaseDeDatosIgualQueElLocal() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        arrancarBot().ensureCurrent(G);
        String enBd = store.readArchivedCsv(G, 0).orElseThrow();
        assertTrue(enBd.contains("1,ana,1120,7,3,10,70.00"), enBd);
        assertEquals("\uFEFF" + enBd, csv(0));
        assertEquals(0, store.latestArchivedSeason(G).getAsLong());
    }

    @Test
    void siSeBorraElCsvLocalSeRecuperaDeLaBaseDeDatos() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        SeasonManager bot = arrancarBot();
        bot.ensureCurrent(G);
        String original = csv(0);
        Files.delete(archive.find(G, 0).orElseThrow());

        assertTrue(archive.find(G, 0).isEmpty());
        Path recuperado = bot.findCsv(G, 0).orElseThrow();
        assertEquals(original, new String(Files.readAllBytes(recuperado), StandardCharsets.UTF_8));
        assertEquals(0, bot.latestArchivedSeason(G).getAsLong());
        assertTrue(bot.findCsv(G, 5).isEmpty());
    }

    @Test
    void siSeCaeAMitadYArrancaEnOtraMaquinaElCsvNoSeRegeneraConDatosReiniciados() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        arrancarBot().ensureCurrent(G);
        store.putPlayer(G, "ana", 1064, 2, 0);
        store.putPlayer(G, "luis", 968, 0, 2);
        today.set(LocalDate.of(2027, 2, 23));

        store.failNextResets = 1;
        assertThrows(RuntimeException.class, () -> arrancarBot().ensureCurrent(G));

        // Otra máquina: carpeta local vacía
        Path otraCarpeta = Files.createTempDirectory("otra-maquina");
        try {
            SeasonArchive otroDisco = new SeasonArchive(otraCarpeta);
            SeasonManager otroBot = new SeasonManager(store, otroDisco, today::get);
            assertEquals(1, otroBot.ensureCurrent(G).orElseThrow().closedSeason());
            String csv = otroDisco.readCsv(otroDisco.find(G, 1).orElseThrow());
            assertTrue(csv.contains("1,ana,1064,2,0,2,100.00"), csv);
            assertTrue(csv.contains("2,luis,968,0,2,2,0.00"), csv);
            assertEquals(1000, store.player(G, "luis").elo());
        } finally {
            try (Stream<Path> files = Files.walk(otraCarpeta)) {
                files.sorted(Collections.reverseOrder()).forEach(f -> f.toFile().delete());
            }
        }
    }

    @Test
    void unCierreEmpezadoPorLaVersionAnteriorSubeSuCsvLocalSinRegenerarlo() throws Exception {
        // Versión anterior: CLOSING con el CSV solo en disco y el reinicio a medias
        archive.write(G, 0, SeasonCsv.build(List.of(new SeasonCsv.Row("ana", 1120, 7, 3))));
        store.states.put(G, new SeasonStore.Stored(0, SeasonStore.Status.CLOSING, 0));
        store.putPlayer(G, "ana", 1000, 0, 0);
        today.set(LocalDate.of(2026, 11, 23));

        arrancarBot().ensureCurrent(G);
        assertTrue(store.readArchivedCsv(G, 0).orElseThrow().contains("ana,1120"));
        assertEquals(1, store.states.get(G).currentSeason());
    }

    @Test
    void elHistoricoListaLasTemporadasYSeLeeSuClasificacion() throws Exception {
        today.set(LocalDate.of(2026, 11, 23));
        SeasonManager bot = arrancarBot();
        bot.ensureCurrent(G);
        store.putPlayer(G, "luis", 1040, 3, 0);
        today.set(LocalDate.of(2027, 2, 23));
        bot.ensureCurrent(G);

        assertEquals(List.of(1L, 0L), bot.archivedSeasons(G));
        List<SeasonCsv.Row> t0 = SeasonCsv.parse(bot.readCsv(G, 0).orElseThrow());
        assertEquals(new SeasonCsv.Row("ana", 1120, 7, 3), t0.get(0));
        List<SeasonCsv.Row> t1 = SeasonCsv.parse(bot.readCsv(G, 1).orElseThrow());
        assertEquals(new SeasonCsv.Row("luis", 1040, 3, 0), t1.get(0));
        assertTrue(bot.readCsv(G, 7).isEmpty());

        // Sin caché local (otro equipo): se lee igual desde la base de datos
        Files.delete(archive.find(G, 1).orElseThrow());
        SeasonManager otro = new SeasonManager(store, new SeasonArchive(Files.createTempDirectory("vacio")), today::get);
        assertEquals(List.of(1L, 0L), otro.archivedSeasons(G));
        assertEquals(t1, SeasonCsv.parse(otro.readCsv(G, 1).orElseThrow()));
    }
}
