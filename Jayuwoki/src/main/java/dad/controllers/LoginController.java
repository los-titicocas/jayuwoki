package dad.controllers;

import dad.custom.ui.CustomTitleBar;
import dad.custom.ui.SplashScreenController;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.awt.*;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class LoginController implements Initializable {

    @FXML
    private BorderPane root;

    @FXML
    void onAccessAction(ActionEvent event) {
        // close login window
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();

        // open main window
        SplashScreenController splashScreenController = new SplashScreenController();
        Scene scene = new Scene(splashScreenController.getRoot());
        Stage mainStage = new Stage();
        mainStage.initStyle(StageStyle.UNDECORATED);
        mainStage.setScene(scene);
        mainStage.show();
        fadeIn(splashScreenController.getRoot());
    }

    @FXML
    void onLinkAction(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URL("https://dam-dad.github.io/jayuwoki/").toURI());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LoginController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setTop(new CustomTitleBar().getRoot());}

    public void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.seconds(2), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public BorderPane getRoot() {
        return root;
    }
}
