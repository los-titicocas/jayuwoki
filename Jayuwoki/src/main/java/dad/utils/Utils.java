package dad.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Utils {

    public static final String CONFIG_FILE = "Jayuwoki/settings.properties";
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
        if (!file.exists()) {
            Properties properties = new Properties();
            try (FileOutputStream output = new FileOutputStream(filePath)) {
                properties.setProperty("theme", "dark");
                properties.setProperty("language", "en");
                properties.store(output, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}