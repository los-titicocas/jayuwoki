package dad.season;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeasonCsvTest {

    @Test
    void ordenaPorEloYCalculaPartidasYWinrate() {
        String csv = SeasonCsv.build(List.of(
                new SeasonCsv.Row("luis", 980, 1, 3),
                new SeasonCsv.Row("ana", 1100, 3, 1),
                new SeasonCsv.Row("nuevo", 1000, 0, 0)));
        String[] lines = csv.split("\r\n");
        assertEquals(SeasonCsv.HEADER, lines[0]);
        assertEquals("1,ana,1100,3,1,4,75.00", lines[1]);
        assertEquals("2,nuevo,1000,0,0,0,0.00", lines[2]);
        assertEquals("3,luis,980,1,3,4,25.00", lines[3]);
    }

    @Test
    void escapaComasComillasYFormulas() {
        assertEquals("\"a,b\"", SeasonCsv.escape("a,b"));
        assertEquals("\"di \"\"hola\"\"\"", SeasonCsv.escape("di \"hola\""));
        assertEquals("'=SUM(A1)", SeasonCsv.escape("=SUM(A1)"));
        assertEquals("pepe", SeasonCsv.escape("pepe"));
    }
}
