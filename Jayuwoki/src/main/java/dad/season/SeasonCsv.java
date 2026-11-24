package dad.season;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generación del CSV con la clasificación final de una temporada. Clase pura para poder testearla.
 * Formato: separador coma, decimales con punto, UTF-8 con BOM (para que Excel lea bien las tildes).
 */
public final class SeasonCsv {

    public static final String HEADER = "posicion,jugador,elo,victorias,derrotas,partidas,winrate";

    /** Fila de la clasificación (lo que se guarda de cada jugador). */
    public record Row(String name, int elo, int wins, int losses) {
        public int games() {
            return wins + losses;
        }

        public double winRate() {
            return games() == 0 ? 0.0 : wins * 100.0 / games();
        }
    }

    /** Orden: ELO desc, victorias desc, nombre asc (desempate determinista). */
    public static final Comparator<Row> RANKING = Comparator
            .comparingInt(Row::elo).reversed()
            .thenComparing(Comparator.comparingInt(Row::wins).reversed())
            .thenComparing(r -> r.name() == null ? "" : r.name());

    private SeasonCsv() {
    }

    /** Filas ordenadas según la clasificación. */
    public static List<Row> rank(List<Row> rows) {
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(RANKING);
        return sorted;
    }

    public static String build(List<Row> rows) {
        List<Row> sorted = rank(rows);
        StringBuilder sb = new StringBuilder(HEADER).append("\r\n");
        int position = 1;
        for (Row r : sorted) {
            sb.append(position++).append(',')
              .append(escape(r.name())).append(',')
              .append(r.elo()).append(',')
              .append(r.wins()).append(',')
              .append(r.losses()).append(',')
              .append(r.games()).append(',')
              .append(String.format(Locale.ROOT, "%.2f", r.winRate()))
              .append("\r\n");
        }
        return sb.toString();
    }

    public static byte[] toBytes(String csv) {
        return ("﻿" + csv).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Escapado RFC 4180 + protección contra "CSV injection" (un nombre que empiece por = + - @
     * se abriría en Excel como fórmula, así que se le antepone un apóstrofo).
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /**
     * Lee un CSV generado por {@link #build} y devuelve las filas en el orden del archivo (ya viene ordenado
     * por la clasificación). Tolera BOM, saltos CRLF/LF, campos entre comillas y el apóstrofo anti-fórmulas.
     */
    public static List<Row> parse(String csv) {
        List<Row> rows = new ArrayList<>();
        if (csv == null) {
            return rows;
        }
        String text = csv.startsWith("\uFEFF") ? csv.substring(1) : csv;
        String[] lines = text.split("\r?\n");
        for (int i = 1; i < lines.length; i++) { // la 0 es la cabecera
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> f = splitLine(lines[i]);
            if (f.size() < 5) {
                continue;
            }
            String name = f.get(1);
            if (name.length() > 1 && name.charAt(0) == '\'' && "=+-@\t\r".indexOf(name.charAt(1)) >= 0) {
                name = name.substring(1);
            }
            try {
                rows.add(new Row(name, Integer.parseInt(f.get(2).trim()),
                        Integer.parseInt(f.get(3).trim()), Integer.parseInt(f.get(4).trim())));
            } catch (NumberFormatException e) {
                // fila corrupta: se ignora
            }
        }
        return rows;
    }

    private static List<String> splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
