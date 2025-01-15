package dad.custom.ui;

import dad.api.Bot;
import dad.controllers.MainController;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class SplashScreenController implements Initializable {

    @FXML
    private AnchorPane root;


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
        // loads the splash screen for 2 seconds and then loads the main window
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> {
            try {
                Stage mainStage = new Stage();
                MainController mainController = new MainController(mainStage);
                Scene scene = new Scene(mainController.getRoot());
                mainStage.initStyle(StageStyle.UNDECORATED);
                mainStage.setScene(scene);
                mainStage.show();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        });
        pause.play();
    }

    public AnchorPane getRoot() {
        return root;
    }
}