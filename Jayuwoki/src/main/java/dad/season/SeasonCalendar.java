package dad.season;

import dad.utils.Utils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Calendario de temporadas. Clase pura (sin Firebase ni Discord) para poder testearla.
 *
 * <ul>
 *   <li>La Temporada 1 empieza el {@link #FIRST_SEASON_START 23/11/2026} y cada temporada dura
 *       {@link #SEASON_LENGTH_MONTHS 3 meses} exactos.</li>
 *   <li>La temporada {@code n} cubre el intervalo semiabierto {@code [startOf(n), endExclusive(n))}:
 *       empieza a las 00:00 de su primer día y termina a las 00:00 del primer día de la siguiente
 *       (en la zona horaria {@link #zone()}). Así la siguiente empieza justo cuando acaba la anterior.</li>
 *   <li>Las fechas se calculan siempre desde la fecha base ({@code base + 3·(n-1) meses}) y no encadenando
 *       "fin anterior + 3 meses", para que nunca se acumule desfase.</li>
 *   <li>Antes del 23/11/2026 estamos en la "pretemporada" (temporada 0): son los datos que ya existían.</li>
 * </ul>
 *
 * Resultado: T1 23 nov 2026 – 22 feb 2027, T2 23 feb – 22 may 2027, T3 23 may – 22 ago 2027, T4 23 ago – 22 nov 2027…
 */
public final class SeasonCalendar {

    public static final LocalDate FIRST_SEASON_START = LocalDate.of(2026, 11, 23);
    public static final int SEASON_LENGTH_MONTHS = 3;
    /** Número de la "pretemporada": los datos anteriores a la Temporada 1. */
    public static final long PRESEASON = 0;

    /**
     * ⚠️ SUPUESTO: el proyecto no definía ninguna zona horaria (usaba la del sistema con LocalDate.now()).
     * Se usa Atlantic/Canary por defecto (el settings.properties del repo está fechado en "WEST", hora canaria)
     * y se puede cambiar con la propiedad {@code season.timezone} de settings.properties (p. ej. Europe/Madrid).
     */
    public static final String DEFAULT_TIMEZONE = "Atlantic/Canary";

    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"));

    private SeasonCalendar() {
    }

    /** Zona horaria usada para decidir en qué día estamos. */
    public static ZoneId zone() {
        String id = Utils.properties.getProperty("season.timezone", DEFAULT_TIMEZONE).trim();
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            System.err.println("⚠️ season.timezone inválida ('" + id + "'), usando " + DEFAULT_TIMEZONE);
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    public static LocalDate today() {
        return LocalDate.now(zone());
    }

    /** Temporada a la que pertenece {@code date} (0 = pretemporada). */
    public static long seasonAt(LocalDate date) {
        if (date.isBefore(FIRST_SEASON_START)) {
            return PRESEASON;
        }
        long months = ChronoUnit.MONTHS.between(FIRST_SEASON_START, date);
        long n = months / SEASON_LENGTH_MONTHS + 1;
        // Ajuste defensivo por si MONTHS.between redondea en algún borde
        while (date.isBefore(startOf(n))) {
            n--;
        }
        while (!date.isBefore(endExclusive(n))) {
            n++;
        }
        return n;
    }

    /** Primer día de la temporada {@code n} (n ≥ 1). */
    public static LocalDate startOf(long n) {
        if (n < 1) {
            throw new IllegalArgumentException("La pretemporada no tiene fecha de inicio definida");
        }
        return FIRST_SEASON_START.plusMonths(SEASON_LENGTH_MONTHS * (n - 1));
    }

    /** Fecha (exclusiva) en que termina la temporada {@code n}: el primer día de la siguiente. */
    public static LocalDate endExclusive(long n) {
        return n < 1 ? FIRST_SEASON_START : startOf(n + 1);
    }

    /** Último día jugable (inclusivo) de la temporada {@code n}. */
    public static LocalDate lastDay(long n) {
        return endExclusive(n).minusDays(1);
    }

    public static String human(LocalDate date) {
        return date.format(HUMAN);
    }

    /** Nombre del CSV de una temporada: {@code temporada-01_2026-11-23_a_2027-02-22.csv}. */
    public static String csvFileName(long n) {
        if (n == PRESEASON) {
            return String.format(Locale.ROOT, "temporada-00_pretemporada_hasta_%s.csv", lastDay(PRESEASON));
        }
        return String.format(Locale.ROOT, "temporada-%02d_%s_a_%s.csv", n, startOf(n), lastDay(n));
    }

    /** Prefijo común de los CSV de la temporada {@code n} (para buscarlos aunque cambie el formato de fechas). */
    public static String csvPrefix(long n) {
        return String.format(Locale.ROOT, "temporada-%02d_", n);
    }

    /** Texto para la cabecera de $verElo. */
    public static String header(LocalDate today) {
        long n = seasonAt(today);
        if (n == PRESEASON) {
            long days = ChronoUnit.DAYS.between(today, FIRST_SEASON_START);
            return String.format("Pretemporada — la Temporada 1 empieza el %s (%s)",
                    human(FIRST_SEASON_START), days == 1 ? "falta 1 día" : "faltan " + days + " días");
        }
        long daysLeft = ChronoUnit.DAYS.between(today, endExclusive(n)); // incluye el día de hoy
        return String.format("Temporada %d (%s - %s) — %d %s",
                n, human(startOf(n)), human(lastDay(n)), daysLeft, daysLeft == 1 ? "día restante" : "días restantes");
    }
}
