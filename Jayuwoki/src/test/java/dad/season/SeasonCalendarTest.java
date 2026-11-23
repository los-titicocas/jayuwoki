package dad.season;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SeasonCalendarTest {

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    void laPrimeraTemporadaEmpiezaEl23DeNoviembreDe2026() {
        assertEquals(0, SeasonCalendar.seasonAt(d(2026, 11, 22)));
        assertEquals(1, SeasonCalendar.seasonAt(d(2026, 11, 23)));
        assertEquals(d(2026, 11, 23), SeasonCalendar.startOf(1));
        assertEquals(d(2027, 2, 22), SeasonCalendar.lastDay(1));
        assertEquals(d(2027, 2, 23), SeasonCalendar.endExclusive(1));
    }

    @Test
    void cadaTemporadaDura3MesesYEmpiezaJustoAlAcabarLaAnterior() {
        assertEquals(1, SeasonCalendar.seasonAt(d(2027, 2, 22)));
        assertEquals(2, SeasonCalendar.seasonAt(d(2027, 2, 23)));
        assertEquals(3, SeasonCalendar.seasonAt(d(2027, 5, 23)));
        assertEquals(4, SeasonCalendar.seasonAt(d(2027, 8, 23)));
        assertEquals(5, SeasonCalendar.seasonAt(d(2027, 11, 23)));
        for (long n = 1; n <= 40; n++) {
            assertEquals(SeasonCalendar.FIRST_SEASON_START.plusMonths(3 * (n - 1)), SeasonCalendar.startOf(n));
            assertEquals(SeasonCalendar.startOf(n + 1), SeasonCalendar.lastDay(n).plusDays(1), "sin huecos ni solapes");
            assertEquals(n, SeasonCalendar.seasonAt(SeasonCalendar.startOf(n)));
            assertEquals(n, SeasonCalendar.seasonAt(SeasonCalendar.lastDay(n)));
        }
    }

    @Test
    void todosLosDiasPertenecenAUnaSolaTemporada() {
        LocalDate day = d(2026, 11, 1);
        long previous = 0;
        while (day.isBefore(d(2032, 1, 1))) {
            long s = SeasonCalendar.seasonAt(day);
            assertTrue(s == previous || s == previous + 1, "salto en " + day);
            if (s >= 1) {
                assertFalse(day.isBefore(SeasonCalendar.startOf(s)));
                assertTrue(day.isBefore(SeasonCalendar.endExclusive(s)));
            }
            previous = s;
            day = day.plusDays(1);
        }
    }

    @Test
    void nombresDeFichero() {
        assertEquals("temporada-01_2026-11-23_a_2027-02-22.csv", SeasonCalendar.csvFileName(1));
        assertEquals("temporada-02_2027-02-23_a_2027-05-22.csv", SeasonCalendar.csvFileName(2));
        assertEquals("temporada-00_pretemporada_hasta_2026-11-22.csv", SeasonCalendar.csvFileName(0));
        assertTrue(SeasonCalendar.csvFileName(12).startsWith(SeasonCalendar.csvPrefix(12)));
    }

    @Test
    void cabecera() {
        assertTrue(SeasonCalendar.header(d(2026, 11, 22)).startsWith("Pretemporada"));
        assertTrue(SeasonCalendar.header(d(2026, 11, 22)).contains("falta 1 día"));
        String h = SeasonCalendar.header(d(2027, 2, 22));
        assertTrue(h.startsWith("Temporada 1 "), h);
        assertTrue(h.endsWith("— 1 día restante"), h);
        assertTrue(SeasonCalendar.header(d(2026, 11, 23)).endsWith("— 92 días restantes"));
    }
}
