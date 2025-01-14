package dad.controllers;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private int tutorialStep = 0;
    private final Map<Integer, PopOver> tutorialPopOvers = new HashMap<>();

    @FXML
    private VBox aboutBox;

    @FXML
    private Button aboutButton;

    @FXML
    private TextFlow aboutInfo;

    @FXML
    private VBox connectBox;

    @FXML
    private Button connectButton;

    @FXML
    private TextFlow connectInfo;

    @FXML
    private VBox contactBox;

    @FXML
    private Button contactButton;

    @FXML
    private TextFlow contactInfo;

    @FXML
    private StackPane contentPane;

    @FXML
    private BorderPane borderPaneRoot;

    @FXML
    private Button logButton;

    @FXML
    private VBox logsBox;

    @FXML
    private TextFlow logsInfo;

    @FXML
    private StackPane root;

    @FXML
    private VBox settingsBox;

    @FXML
    private Button settingsButton;

    @FXML
    private TextFlow settingsInfo;

    @FXML
    private SplitPane splitPaneRoot;

    @FXML
    private Button tutorialButton;

    @FXML
    private AnchorPane tutorialPane;


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
        borderPaneRoot.setTop(new CustomTitleBar().getRoot());

        // Initialize text for each TextFlow
        addTutorialText();

        // Initialize popOvers
        popOverMap();

        // Disable tutorial box and set tutorialPane to invisible
        disableTutorialBox();
        tutorialPane.setVisible(false);
        onCollapsedMenu();

        // add a listener to the menu buttons to disable while the menu is collapsed
        buttonsListener();
    }

    private void popOverMap() {
        tutorialPopOvers.put(0, createPopOver(connectBox));
        tutorialPopOvers.put(1, createPopOver(settingsBox));
        tutorialPopOvers.put(2, createPopOver(logsBox));
        tutorialPopOvers.put(3, createPopOver(aboutBox));
        tutorialPopOvers.put(4, createPopOver(contactBox));
    }

    private void buttonsListener() {
        connectButton.disableProperty().bind(splitPaneRoot.getDividers().getFirst().positionProperty().lessThan(0.15));
        settingsButton.disableProperty().bind(splitPaneRoot.getDividers().getFirst().positionProperty().lessThan(0.15));
        logButton.disableProperty().bind(splitPaneRoot.getDividers().getFirst().positionProperty().lessThan(0.15));
        aboutButton.disableProperty().bind(splitPaneRoot.getDividers().getFirst().positionProperty().lessThan(0.15));
        contactButton.disableProperty().bind(splitPaneRoot.getDividers().getFirst().positionProperty().lessThan(0.15));
    }

    private void addTutorialText() {
        // TODO:add to css later on
        Text connectText = new Text("This button connects to the server.");
        connectText.setStyle("-fx-font-size: 14; -fx-fill: black;");
        connectInfo.getChildren().add(connectText);

        Text settingsText = new Text("This button opens the settings.");
        settingsText.setStyle("-fx-font-size: 14; -fx-fill: black;");
        settingsInfo.getChildren().add(settingsText);

        Text logsText = new Text("This button shows the logs.");
        logsText.setStyle("-fx-font-size: 14; -fx-fill: black;");
        logsInfo.getChildren().add(logsText);

        Text aboutText = new Text("This button shows information about the application.");
        aboutText.setStyle("-fx-font-size: 14; -fx-fill: black;");
        aboutInfo.getChildren().add(aboutText);

        Text contactText = new Text("This button opens the contact form.");
        contactText.setStyle("-fx-font-size: 14; -fx-fill: black;");
        contactInfo.getChildren().add(contactText);
    }

    private PopOver createPopOver(VBox content) {
        PopOver popOver = new PopOver(content);
        popOver.styleProperty().setValue("-fx-background-color: orange;");
        popOver.setOpacity(0.6);
        popOver.autoHideProperty().setValue(false);
        popOver.setArrowLocation(PopOver.ArrowLocation.LEFT_CENTER);
        popOver.setDetachable(false);
        return popOver;
    }

    private void disableTutorialBox() {
        aboutBox.setVisible(false);
        settingsBox.setVisible(false);
        logsBox.setVisible(false);
        contactBox.setVisible(false);
        connectBox.setVisible(false);
    }

    private void onCollapsedMenu() {
        splitPaneRoot.getDividers().getFirst().positionProperty().addListener((obs, oldVal, newVal) -> {
            tutorialButton.setDisable(newVal.doubleValue() < 0.2);
        });
    }

    @FXML
    void onCollapseAction(ActionEvent event) {
        if (splitPaneRoot.getDividerPositions() != null && splitPaneRoot.getDividerPositions()[0] > 0.2) {
            splitPaneRoot.setDividerPositions(0);
        } else if (splitPaneRoot.getDividerPositions() != null && splitPaneRoot.getDividerPositions()[0] < 0.2) {
            splitPaneRoot.setDividerPositions(0.2);
        }
    }

    @FXML
    void onTutorialAction(ActionEvent event) {
        tutorialStep = 0;
        showTutorialStep(tutorialStep);
    }

    @FXML
    void onNextAction(ActionEvent event) {
        tutorialStep++;
        showTutorialStep(tutorialStep);
        tutorialPopOvers.get(tutorialStep - 1).hide();

        if (tutorialStep == 5) {
            tutorialPane.setVisible(false);
        }
    }

    private void showTutorialStep(int step) {
        disableTutorialBox();

        PopOver currentPopOver = tutorialPopOvers.get(step);
        if (currentPopOver != null) {
            switch (step) {
                case 0:
                    connectBox.setVisible(true);
                    currentPopOver.show(connectButton);
                    break;
                case 1:
                    settingsBox.setVisible(true);
                    currentPopOver.show(settingsButton);
                    break;
                case 2:
                    logsBox.setVisible(true);
                    currentPopOver.show(logButton);
                    break;
                case 3:
                    aboutBox.setVisible(true);
                    currentPopOver.show(aboutButton);
                    break;
                case 4:
                    contactBox.setVisible(true);
                    currentPopOver.show(contactButton);
                    break;
            }
        }

        tutorialPane.setVisible(true);
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

    private void loadContent(String fxmlPath) {
        try {
            StackPane pane = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(pane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public VBox getAboutBox() {
        return aboutBox;
    }

    public VBox getConnectBox() {
        return connectBox;
    }

    public VBox getContactBox() {
        return contactBox;
    }

    public StackPane getContentPane() {
        return contentPane;
    }

    public VBox getLogsBox() {
        return logsBox;
    }

    public StackPane getRoot() {
        return root;
    }

    public VBox getSettingsBox() {
        return settingsBox;
    }

    public SplitPane getSplitPaneRoot() {
        return splitPaneRoot;
    }

    public Button getTutorialButton() {
        return tutorialButton;
    }

    public AnchorPane getTutorialPane() {
        return tutorialPane;
    }
}