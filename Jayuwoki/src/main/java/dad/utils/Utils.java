package dad.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Utils {

    public static final String CONFIG_FILE = "Jayuwoki/settings.properties";
    public static Properties properties = new Properties();

    public static void saveProperties() {
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, null);
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
    }
}
