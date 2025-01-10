package dad.controllers;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.awt.*;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LoginController implements Initializable {

    @FXML
    private BorderPane root;

    @FXML
    void onAccessAction(ActionEvent event) {
        // close login window
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();

        // open main window
        MainController mainController = new MainController();
        Scene scene = new Scene(mainController.getRoot());
        Stage mainStage = new Stage();
        mainStage.setTitle("JayuwokiBot");
        mainStage.setScene(scene);
        mainStage.show();
    }

    @FXML
    void onLinkAction(ActionEvent event) {
        try {
            Desktop.getDesktop().browse(new URL("http://www.github.com/dam-dad/jayuwoki").toURI());
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

    }

    public BorderPane getRoot() {
        return root;
    }
}
