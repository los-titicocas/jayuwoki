package dad.season;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Caché local de los CSV de las temporadas cerradas (la copia buena está en Firestore), uno por servidor:
 * {@code ~/.jayuwoki/temporadas/<guildId>/temporada-01_2026-11-23_a_2027-02-22.csv}
 *
 * ⚠️ SUPUESTO: se guarda junto al settings.properties (~/.jayuwoki), en el equipo donde corre el bot.
 * Se puede cambiar con la propiedad {@code season.archiveDir}.
 */
public class SeasonArchive {

    private static final Pattern FILE = Pattern.compile("temporada-(\\d+)_.*\\.csv");

    private final Path baseDir;

    public SeasonArchive(Path baseDir) {
        this.baseDir = baseDir;
    }

    public static Path defaultDir(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim());
        }
        return Paths.get(System.getProperty("user.home"), ".jayuwoki", "temporadas");
    }

    private Path guildDir(String guildId) {
        // El guildId de Discord es numérico; se filtra por si acaso para no salir de la carpeta.
        return baseDir.resolve(guildId.replaceAll("[^0-9A-Za-z_-]", "_"));
    }

    public boolean exists(String guildId, long season) {
        return find(guildId, season).isPresent();
    }

    /** CSV de la temporada indicada, si existe. */
    public Optional<Path> find(String guildId, long season) {
        Path dir = guildDir(guildId);
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        String prefix = SeasonCalendar.csvPrefix(season);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, prefix + "*.csv")) {
            for (Path p : files) {
                return Optional.of(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /** Número de la última temporada archivada en este servidor. */
    public OptionalLong latestSeason(String guildId) {
        File[] files = guildDir(guildId).toFile().listFiles();
        if (files == null) {
            return OptionalLong.empty();
        }
        long best = -1;
        for (File f : files) {
            Matcher m = FILE.matcher(f.getName());
            if (m.matches()) {
                best = Math.max(best, Long.parseLong(m.group(1)));
            }
        }
        return best < 0 ? OptionalLong.empty() : OptionalLong.of(best);
    }

    /**
     * Escribe el CSV de forma atómica (fichero temporal + move): o queda el CSV completo, o no queda nada.
     * Así nunca queda un CSV a medias en la caché.
     */
    public Path write(String guildId, long season, String csv) throws IOException {
        Path dir = Files.createDirectories(guildDir(guildId));
        Path target = dir.resolve(SeasonCalendar.csvFileName(season));
        Path tmp = Files.createTempFile(dir, ".tmp-", ".csv");
        try {
            Files.write(tmp, SeasonCsv.toBytes(csv));
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
        return target;
    }

    /** Lee un CSV escrito por {@link #write} (quitando el BOM). */
    public String readCsv(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
}
