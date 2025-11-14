package dad.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Properties;

public class Utils {

    // Ruta absoluta basada en user.home para evitar problemas con Maven
    public static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + ".jayuwoki" + File.separator + "settings.properties";
    public static Properties properties = new Properties();

    public static void saveProperties() {
        Properties existingProperties = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            existingProperties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        existingProperties.putAll(properties);

        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            existingProperties.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadProperties() {
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setTheme(String theme) {
        properties.setProperty("theme", theme);
        saveProperties();
        System.out.println("Theme saved: " + theme); // Debugging line
    }

    public static void createPropertiesFile(String filePath) {
        File file = new File(filePath);
        
        // Crear directorio padre si no existe
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            System.out.println("📁 Directory created: " + parentDir.getAbsolutePath() + " → " + created);
        }
        
        if (!file.exists()) {
            Properties properties = new Properties();
            try (FileOutputStream output = new FileOutputStream(filePath)) {
                properties.setProperty("theme", "dark");
                properties.setProperty("language", "en");
                properties.setProperty("massPermissionCheck", "false");
                properties.setProperty("imageCheck", "false");
                properties.setProperty("soundCheck", "false");
                properties.setProperty("rollaDie", "true");
                // Sistema de permisos jerárquico
                properties.setProperty("bot.developers", ""); // IDs separados por comas
                properties.setProperty("trusted.users", ""); // IDs separados por comas
                properties.store(output, null);
                System.out.println("✅ Settings file created: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("❌ Error creating settings file:");
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ Settings file already exists: " + file.getAbsolutePath());
        }
    }

    /**
     * Calcula la temporada actual basándose en un inicio fijo (temporada 1 empieza el 2 de noviembre de 2025)
     * y periodos de 4 meses. Devuelve un encabezado legible con rango y días restantes.
     */
    public static String getSeasonHeader() {
        // Base: la primera temporada (Season 1) empieza el 2 de noviembre de 2025
        LocalDate baseStart = LocalDate.of(2025, 11, 2);
        LocalDate today = LocalDate.now();

        // Calcular cuántos meses completos han pasado desde la base (en términos de inicio de mes)
        long monthsBetween = ChronoUnit.MONTHS.between(baseStart.withDayOfMonth(1), today.withDayOfMonth(1));
        long periods = monthsBetween >= 0 ? monthsBetween / 4 : (long) Math.floor(monthsBetween / 4.0);

        LocalDate seasonStart = baseStart.plusMonths(periods * 4);
        LocalDate seasonEnd = seasonStart.plusMonths(4).minusDays(1);

        // Si hoy ya pasó el seasonEnd (caso borde por redondeo), avanzar un periodo
        if (today.isAfter(seasonEnd)) {
            periods++;
            seasonStart = baseStart.plusMonths(periods * 4);
            seasonEnd = seasonStart.plusMonths(4).minusDays(1);
        }

        int seasonNumber = (int) (1 + periods);

        long daysLeft = ChronoUnit.DAYS.between(today, seasonEnd) + 1; // incluir día final
        if (daysLeft < 0) daysLeft = 0;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es"));
        String startStr = seasonStart.format(fmt);
        String endStr = seasonEnd.format(fmt);

        return String.format("Temporada %d (%s - %s) — %d días restantes", seasonNumber, startStr, endStr, daysLeft);
    }
}