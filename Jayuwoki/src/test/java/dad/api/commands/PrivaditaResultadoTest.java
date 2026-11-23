package dad.api.commands;

import dad.database.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PrivaditaResultadoTest {

    private static List<Player> diezJugadores() {
        int[] elos = {1100, 1050, 1000, 980, 1200, 990, 1010, 1030, 1150, 1060};
        List<Player> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Player p = new Player();
            p.setName("j" + i);
            p.setElo(elos[i]);
            p.setWins(i);
            p.setLosses(10 - i);
            list.add(p);
        }
        return list;
    }

    @Test
    void parseaRedYBlueSinImportarMayusculas() {
        for (String s : List.of("red", "RED", "Red", "rEd", " red ", "\tRED\n")) {
            assertEquals(Optional.of(Privadita.Team.RED), Privadita.Team.parse(s), s);
        }
        for (String s : List.of("blue", "BLUE", "Blue", "bLuE", "  Blue")) {
            assertEquals(Optional.of(Privadita.Team.BLUE), Privadita.Team.parse(s), s);
        }
    }

    @Test
    void rechazaEntradasInvalidas() {
        for (String s : new String[]{null, "", "   ", "azul", "rojo", "reds", "b", "blue red", "@everyone"}) {
            assertTrue(Privadita.Team.parse(s).isEmpty(), String.valueOf(s));
        }
    }

    @Test
    void aplicaElResultadoIgualQueElAlgoritmoOriginal() {
        List<Player> jugadores = diezJugadores();
        // Esperado: fórmula original (cada jugador contra la media del equipo rival, calculada antes de cambiar nada)
        double mediaBlue = jugadores.subList(0, 5).stream().mapToInt(Player::getElo).average().orElse(0);
        double mediaRed = jugadores.subList(5, 10).stream().mapToInt(Player::getElo).average().orElse(0);
        int[] esperado = new int[10];
        for (int i = 0; i < 10; i++) {
            Player copia = new Player();
            copia.setElo(jugadores.get(i).getElo());
            boolean esRed = i >= 5;
            copia.ActualizarElo(esRed ? mediaBlue : mediaRed, esRed); // gana Red
            esperado[i] = copia.getElo();
        }

        Privadita privadita = new Privadita(jugadores);
        String mensaje = privadita.aplicarResultado(Privadita.Team.parse("ReD").orElseThrow());

        assertTrue(mensaje.contains("Equipo Rojo ha ganado"));
        for (int i = 0; i < 10; i++) {
            Player p = jugadores.get(i);
            assertEquals(esperado[i], p.getElo(), "elo " + p.getName());
            if (i >= 5) {
                assertEquals(i + 1, p.getWins());
                assertEquals(10 - i, p.getLosses());
            } else {
                assertEquals(i, p.getWins());
                assertEquals(10 - i + 1, p.getLosses());
            }
        }
    }

    @Test
    void entradaInvalidaNoTocaNadaYDespuesFuncionaLaValida() {
        List<Player> jugadores = diezJugadores();
        Privadita privadita = new Privadita(jugadores);
        int eloAntes = jugadores.get(0).getElo();

        assertTrue(Privadita.Team.parse("verde").isEmpty()); // Commands responde y no llama a aplicarResultado
        assertEquals(eloAntes, jugadores.get(0).getElo());

        String mensaje = privadita.aplicarResultado(Privadita.Team.parse("BLUE").orElseThrow());
        assertTrue(mensaje.contains("Equipo Azul ha ganado"));
        assertEquals(1, jugadores.get(0).getWins());
        assertTrue(jugadores.get(0).getElo() != eloAntes);
    }

    @Test
    void privaditaIncompletaLanzaErrorSinModificarANadie() {
        List<Player> nueve = diezJugadores().subList(0, 9);
        List<Player> copia = new ArrayList<>(nueve);
        Privadita privadita = new Privadita(copia);
        assertFalse(privadita.isValid());
        int[] elos = copia.stream().mapToInt(Player::getElo).toArray();

        assertThrows(IllegalStateException.class, () -> privadita.aplicarResultado(Privadita.Team.BLUE));
        for (int i = 0; i < copia.size(); i++) {
            assertEquals(elos[i], copia.get(i).getElo());
        }
    }

    @Test
    void restoreDeshaceUnResultadoSiFallaElGuardado() {
        List<Player> jugadores = diezJugadores();
        Privadita privadita = new Privadita(jugadores);
        List<Privadita.StatsSnapshot> antes = privadita.snapshotStats();
        int elo0 = jugadores.get(0).getElo();
        int wins0 = jugadores.get(0).getWins();

        privadita.aplicarResultado(Privadita.Team.BLUE);
        Privadita.restoreStats(antes); // lo que hace Commands si updatePlayers devuelve false

        assertEquals(elo0, jugadores.get(0).getElo());
        assertEquals(wins0, jugadores.get(0).getWins());
        // y se puede registrar de nuevo con normalidad
        privadita.aplicarResultado(Privadita.Team.parse("red").orElseThrow());
        assertEquals(wins0, jugadores.get(0).getWins());
        assertEquals(6, jugadores.get(5).getWins()); // tenía 5
    }
}
