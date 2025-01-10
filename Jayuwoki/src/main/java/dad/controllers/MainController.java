package dad.controllers;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController implements Initializable{

    @FXML
    private StackPane contentPane;

    @FXML
    private VBox menuBox;

    @FXML
    private BorderPane root;

    public MainController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainControllerView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    void onChangeTheme(ActionEvent event) {
        if (Application.getUserAgentStylesheet().equals(new PrimerDark().getUserAgentStylesheet())) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }

    }

    public BorderPane getRoot() {
        return root;
    }

    // allows to load the content of the main window
    private void loadContent(String fxmlPath) {
        try {
            StackPane pane = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(pane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
