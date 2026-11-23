package dad.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
     * Cabecera de temporada que muestran $verElo y $verElo <nombre>.
     * Antes calculaba temporadas de 4 meses desde el 2/11/2025; ahora delega en el sistema de temporadas
     * (Temporada 1 desde el 23/11/2026, 3 meses cada una) para que coincida con los reinicios reales.
     */
    public static String getSeasonHeader() {
        return dad.season.SeasonCalendar.header(dad.season.SeasonCalendar.today());
    }
}