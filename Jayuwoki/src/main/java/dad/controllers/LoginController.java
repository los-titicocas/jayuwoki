package dad.controllers;

import dad.custom.ui.CustomTitleBar;
import dad.custom.ui.SplashScreenController;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.awt.*;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import javafx.scene.paint.Color;

public class LoginController implements Initializable {

    @FXML
    private BorderPane loginRoot;

    @FXML
    void onAccessAction(ActionEvent event) {
        // Close login window
        Stage stage = (Stage) loginRoot.getScene().getWindow();
        stage.close();

        // Open main window (splash screen)
        SplashScreenController splashScreenController = new SplashScreenController();

        // Create scene and stage for splash screen
        Scene scene = new Scene(splashScreenController.getRoot());
        Stage mainStage = new Stage();
        Image appIcon = new Image(Objects.requireNonNull(getClass().getResource("/images/logo.png")).toString());

        mainStage.getIcons().add(appIcon);
        mainStage.initStyle(StageStyle.TRANSPARENT);  // Ensure the splash screen has no border
        mainStage.setScene(scene);

        // Set the scene's background to transparent after it is created
        scene.setFill(Color.TRANSPARENT);  // Make the scene transparent

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
        loginRoot.setTop(new CustomTitleBar().getRoot());}

    public void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.seconds(2), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public BorderPane getRoot() {
        return loginRoot;
    }
}
