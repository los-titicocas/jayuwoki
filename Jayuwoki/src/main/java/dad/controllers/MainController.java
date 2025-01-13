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
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;

public class MainController implements Initializable{

    @FXML
    private StackPane contentPane;

    @FXML
    private BorderPane root;

    @FXML
    private SplitPane splitPaneRoot;

    @FXML
    private Button tutorialButton;

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
        //loadContent("/fxml/DashboardView.fxml"); example to load a menu by default

        onCollapsedMenu();
    }

    // adds a listener to the tutorial button to disable it while the menu is collapsed
    private void onCollapsedMenu() {
        splitPaneRoot.getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
            tutorialButton.setDisable(newVal.doubleValue() < 0.2);
        });
    }

    // side menu actions
    @FXML
    void onCollapseAction(ActionEvent event) {
        if (splitPaneRoot.getDividerPositions() != null && splitPaneRoot.getDividerPositions()[0] > 0.2) {
            splitPaneRoot.setDividerPositions(0);
        } else if(splitPaneRoot.getDividerPositions() != null && splitPaneRoot.getDividerPositions()[0] < 0.2) {
            splitPaneRoot.setDividerPositions(0.3);
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
    void onTutorialAction(ActionEvent event) {

    }

    @FXML
    void onChangeTheme(ActionEvent event) {
        if (Application.getUserAgentStylesheet().equals(new PrimerDark().getUserAgentStylesheet())) {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        }

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

    public StackPane getContentPane() {
        return contentPane;
    }

    public void setContentPane(StackPane contentPane) {
        this.contentPane = contentPane;
    }

    public BorderPane getRoot() {
        return root;
    }

    public void setRoot(BorderPane root) {
        this.root = root;
    }

    public SplitPane getSplitPaneRoot() {
        return splitPaneRoot;
    }

    public void setSplitPaneRoot(SplitPane splitPaneRoot) {
        this.splitPaneRoot = splitPaneRoot;
    }
}
