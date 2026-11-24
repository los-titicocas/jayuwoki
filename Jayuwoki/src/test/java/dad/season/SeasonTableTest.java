package dad.season;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeasonTableTest {

    @Test
    void elCsvSeLeeIgualQueSeEscribio() {
        List<SeasonCsv.Row> rows = List.of(
                new SeasonCsv.Row("guamero", 1120, 26, 10),
                new SeasonCsv.Row("a,b \"raro\"", 1050, 1, 1),
                new SeasonCsv.Row("=formula", 990, 0, 2),
                new SeasonCsv.Row("chris", 1000, 0, 0));
        String csv = SeasonCsv.build(rows);
        List<SeasonCsv.Row> parsed = SeasonCsv.parse("﻿" + csv);
        assertEquals(SeasonCsv.rank(rows), parsed);
        assertEquals(parsed, SeasonCsv.parse(csv.replace("\r\n", "\n"))); // también con saltos LF
    }

    @Test
    void ignoraLineasCorruptasYEntradasVacias() {
        assertTrue(SeasonCsv.parse(null).isEmpty());
        assertTrue(SeasonCsv.parse("").isEmpty());
        List<SeasonCsv.Row> rows = SeasonCsv.parse(SeasonCsv.HEADER + "\r\n1,ana,1100,3,1,4,75.00\r\nbasura\r\n2,luis,xx,1,1,2,50.00\r\n");
        assertEquals(List.of(new SeasonCsv.Row("ana", 1100, 3, 1)), rows);
    }

    @Test
    void laTablaTieneElMismoFormatoQueVerElo() {
        List<String> msgs = SeasonTable.render("📅 Temporada 1", List.of(
                new SeasonCsv.Row("guamero", 1120, 26, 10), new SeasonCsv.Row("chris", 1000, 0, 0)));
        assertEquals(1, msgs.size());
        String m = msgs.get(0);
        assertTrue(m.startsWith("📅 Temporada 1\n\n```\n"), m);
        assertTrue(m.contains(String.format("%-3s %-20s %6s %4s %4s %7s\n", "#", "Jugador", "Elo", "W", "L", "WR")), m);
        assertTrue(m.contains(String.format("%-3d %-20s %6d %4d %4d %6.2f%%\n", 1, "guamero", 1120, 26, 10, 72.2222222)), m);
        assertTrue(m.contains(String.format("%-3d %-20s %6d %4d %4d %6.2f%%\n", 2, "chris", 1000, 0, 0, 0.0)), m);
        assertTrue(m.endsWith("```"), m);
    }

    @Test
    void siNoCabeEnUnMensajeSeParteSinPasarDe2000Caracteres() {
        List<SeasonCsv.Row> rows = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            rows.add(new SeasonCsv.Row("jugador" + i, 1500 - i, i, 3));
        }
        List<String> msgs = SeasonTable.render("📅 Temporada 3 (…) — finalizada\n\n🏆 **CLASIFICACIÓN FINAL - X**", rows);
        assertTrue(msgs.size() > 1);
        int filas = 0;
        for (String m : msgs) {
            assertTrue(m.length() <= SeasonTable.DISCORD_LIMIT, "longitud " + m.length());
            assertEquals(2, m.split("```", -1).length - 1, "cada trozo abre y cierra su bloque de código");
            filas += (int) m.lines().filter(l -> l.matches("\\d+\\s+jugador\\d+.*")).count();
        }
        assertEquals(120, filas);
        assertTrue(msgs.get(msgs.size() - 1).contains("120 "));
    }
}
