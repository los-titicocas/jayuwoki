package dad.controllers;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainController implements Initializable{

    @FXML
    private StackPane contentPane;

    @FXML
    private SplitPane root;

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

    // side menu actions

    @FXML
    void onCollapseAction(ActionEvent event) {
        if (root.getDividerPositions() != null && root.getDividerPositions()[0] > 0.2) {
            root.setDividerPositions(0);
        } else if(root.getDividerPositions() != null && root.getDividerPositions()[0] < 0.2) {
            root.setDividerPositions(0.3);
        }
    }

    @FXML
    void onAboutAction(ActionEvent event) {

    }

    @FXML
    void onConnectAction(ActionEvent event) {

    }

    @FXML
    void onContactAction(ActionEvent event) {

    }

    @FXML
    void onLogsAction(ActionEvent event) {

    }

    @FXML
    void onSettingsAction(ActionEvent event) {

    }

    @FXML
    void onChangeTheme(ActionEvent event) {
        if (Application.getUserAgentStylesheet().equals(new PrimerDark().getUserAgentStylesheet())) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }

    }

    public SplitPane getRoot() {
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
