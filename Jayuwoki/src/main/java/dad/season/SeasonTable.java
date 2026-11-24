package dad.season;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte la clasificación de una temporada en mensajes de Discord con la misma tabla que $verElo.
 * Clase pura para poder testearla.
 */
public final class SeasonTable {

    /** Límite de caracteres de un mensaje de Discord. */
    public static final int DISCORD_LIMIT = 2000;

    private static final String HEADER_ROW = String.format("%-3s %-20s %6s %4s %4s %7s\n", "#", "Jugador", "Elo", "W", "L", "WR");
    private static final String SEPARATOR = "─".repeat(50) + "\n";

    private SeasonTable() {
    }

    /**
     * @param title  texto encima de la tabla (cabecera de temporada y título), en Markdown de Discord
     * @param rows   filas en el orden de la clasificación
     * @return uno o varios mensajes (si la tabla no cabe en 2000 caracteres se parte, repitiendo la cabecera)
     */
    public static List<String> render(String title, List<SeasonCsv.Row> rows) {
        List<String> lines = new ArrayList<>();
        int position = 1;
        for (SeasonCsv.Row r : rows) {
            // Mismo formato que DBManager.ShowAllElo
            lines.add(String.format("%-3d %-20s %6d %4d %4d %6.2f%%\n",
                    position++, r.name(), r.elo(), r.wins(), r.losses(), r.winRate()));
        }

        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder(title).append("\n\n```\n").append(HEADER_ROW).append(SEPARATOR);
        int rowsInCurrent = 0;
        for (String line : lines) {
            if (rowsInCurrent > 0 && current.length() + line.length() + 3 > DISCORD_LIMIT) {
                messages.add(current.append("```").toString());
                current = new StringBuilder("```\n").append(HEADER_ROW).append(SEPARATOR);
                rowsInCurrent = 0;
            }
            current.append(line);
            rowsInCurrent++;
        }
        if (lines.isEmpty()) {
            current.append("(sin jugadores)\n");
        }
        messages.add(current.append("```").toString());
        return messages;
    }
}
