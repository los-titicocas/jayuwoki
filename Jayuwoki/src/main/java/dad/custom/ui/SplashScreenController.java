package dad.custom.ui;

import dad.controllers.MainController;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import jfxtras.scene.layout.CircularPane;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    @FXML
    private StackPane splashScreenRoot;

    @FXML
    private CircularPane circularPane;

    public SplashScreenController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SplashScreen.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // Apply the circular clip to make the splash screen circular
        Circle clip = new Circle(150); // Half of CircularPane's width/height
        clip.centerXProperty().bind(splashScreenRoot.widthProperty().divide(2));
        clip.centerYProperty().bind(splashScreenRoot.heightProperty().divide(2));
        splashScreenRoot.setClip(clip); // Clip root pane to make it circular

        // Loads the splash screen for 2 seconds and then loads the main window
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> {
            try {
                Stage mainStage = new Stage();
                MainController mainController = new MainController(mainStage);
                Scene sceneMain = new Scene(mainController.getRoot());
                Image appIcon = new Image(Objects.requireNonNull(getClass().getResource("/images/logo.png")).toString());

                mainStage.getIcons().add(appIcon);
                mainStage.initStyle(StageStyle.UNDECORATED); // No border for the main window
                mainStage.setScene(sceneMain);
                mainStage.show();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Stage stage = (Stage) splashScreenRoot.getScene().getWindow();
            stage.close(); // Close splash screen after 2 seconds
        });
        pause.play();
    }

    public StackPane getRoot() {
        return splashScreenRoot;
    }
    public CircularPane getCircularPane() {
        return circularPane;
    }
}
