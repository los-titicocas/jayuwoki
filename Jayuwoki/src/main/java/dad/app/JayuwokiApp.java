package dad.app;

import dad.custom.ui.SplashScreenController;
import dad.utils.Utils;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.Objects;

public class JayuwokiApp extends Application {

    SplashScreenController splashScreenController = new SplashScreenController();

    @Override
    public void start(Stage primaryStage) throws Exception {
        // if the properties file does not exist, create it
        Utils.createPropertiesFile(Utils.CONFIG_FILE);

        // Load properties before accessing them
        Utils.loadProperties();

        // Read theme with fallback
        String theme = Utils.properties.getProperty("theme");
        if (theme == null || theme.isBlank()) {
            System.err.println("Theme property missing, using default 'light'");
            theme = "light";
        }

        // Try to load theme resource; if missing, fall back to Modena
        URL themeUrl = getClass().getResource("/styles/" + theme + "-theme.css");
        if (themeUrl != null) {
            Application.setUserAgentStylesheet(themeUrl.toExternalForm());
        } else {
            System.err.println("Theme resource not found: /styles/" + theme + "-theme.css. Using default stylesheet.");
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
        }

        // Load app icon with null check
        URL iconUrl = getClass().getResource("/images/logo.png");
        if (iconUrl != null) {
            Image appIcon = new Image(iconUrl.toString());
            // Use a new stage for the splash as before
            Scene scene = new Scene(splashScreenController.getRoot());
            Stage stage = new Stage();

            stage.getIcons().add(appIcon);
            stage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            fadeIn(splashScreenController.getRoot());
            stage.show();
        } else {
            System.err.println("Icon resource not found: /images/logo.png. Continuing without icon.");
            Scene scene = new Scene(splashScreenController.getRoot());
            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            fadeIn(splashScreenController.getRoot());
            stage.show();
        }
    }

    public void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.seconds(2), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}
