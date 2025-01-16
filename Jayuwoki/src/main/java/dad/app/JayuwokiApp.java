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

import java.util.Objects;

public class JayuwokiApp extends Application {

    SplashScreenController splashScreenController = new SplashScreenController();

    @Override
    public void start(Stage primaryStage) throws Exception {
        Utils.loadProperties();

        String theme = Utils.properties.getProperty("theme", "dark");
        String themePath = getClass().getResource("/styles/" + theme + "-theme.css").toExternalForm();
        Application.setUserAgentStylesheet(themePath);

        Image appIcon = new Image(Objects.requireNonNull(getClass().getResource("/images/logo.png")).toString());

        Scene scene = new Scene(splashScreenController.getRoot());
        Stage stage = new Stage();

        stage.getIcons().add(appIcon);
        stage.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        fadeIn(splashScreenController.getRoot());
        stage.show();
    }

    public void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.seconds(2), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}