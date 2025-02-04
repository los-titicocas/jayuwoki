package dad.controllers;

import dad.app.JayuwokiApp;
import dad.custom.ui.CustomTitleBar;
import dad.panels.*;
import dad.utils.Utils;
import javafx.application.Application;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.SnapshotResult;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import okhttp3.internal.cache.DiskLruCache;
import org.controlsfx.control.PopOver;

import java.net.URL;
import java.util.*;

public class MainController implements Initializable {

    private int tutorialStep = 0;
    private final Map<Integer, PopOver> tutorialPopOvers = new HashMap<>();

    // Controllers
    private final ContactController contactController = new ContactController();
    private final ConnectController connectController = new ConnectController();
    private final AboutController aboutController = new AboutController();
    private final SettingsController settingsController = new SettingsController();

    @FXML
    private VBox aboutBox;

    @FXML
    private Button aboutButton;

    @FXML
    private TextFlow aboutInfo;

    @FXML
    private BorderPane borderPaneRoot;

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
    private Button logButton;

    @FXML
    private VBox logsBox;

    @FXML
    private TextFlow logsInfo;

    @FXML
    private GridPane menuGridPane;

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
    private Button themeButton;

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
        CustomTitleBar customTitleBar = new CustomTitleBar();
        // Set the bot on the title bar so it can be stopped when the window is closed
        customTitleBar.setBot(connectController.getBot());
        borderPaneRoot.setTop(customTitleBar.getRoot());

        connectController.setMainController(this);

        // Initialize text for each TextFlow
        addTutorialText();

        // Initialize popOvers
        popOverMap();

        // Disable tutorial box and set tutorialPane to invisible
        disableTutorialBox();
        tutorialPane.setVisible(false);
//        onCollapsedMenu();

        // Listener to ensure that the connection is closed by also pressing alt + F4
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F4 && event.isAltDown()) {
                customTitleBar.onCloseWindow();
            }
        });

        // bind setting checkbox
    }

    private void popOverMap() {
        tutorialPopOvers.put(0, createPopOver(connectBox));
        tutorialPopOvers.put(1, createPopOver(settingsBox));
        tutorialPopOvers.put(2, createPopOver(logsBox));
        tutorialPopOvers.put(3, createPopOver(aboutBox));
        tutorialPopOvers.put(4, createPopOver(contactBox));
    }

    private void addTutorialText() {
        Text connectText = new Text("This button connects to the server.");
        connectInfo.getChildren().add(connectText);

        Text settingsText = new Text("This button opens the settings.");
        settingsInfo.getChildren().add(settingsText);

        Text logsText = new Text("This button shows the logs.");
        logsInfo.getChildren().add(logsText);

        Text aboutText = new Text("This button shows information about the application.");
        aboutInfo.getChildren().add(aboutText);

        Text contactText = new Text("This button opens the contact form.");
        contactInfo.getChildren().add(contactText);
    }

    private PopOver createPopOver(VBox content) {
        PopOver popOver = new PopOver(content);
        popOver.styleProperty().setValue("-fx-background-color: orange;");
        popOver.setOpacity(0.9);
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
        System.out.println(Arrays.toString(splitPaneRoot.getDividerPositions()));
        if (splitPaneRoot.getDividerPositions() != null && splitPaneRoot.getDividerPositions()[0] > 0.15) {
            splitPaneRoot.setDividerPositions(0);
        } else if (splitPaneRoot.getDividerPositions() != null && splitPaneRoot.getDividerPositions()[0] < 0.15) {
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
            stopTutorial();
        }
    }

    private void showTutorialStep(int step) {
        disableTutorialBox();
        tutorialPane.setVisible(true);
        tutorialPane.getChildren().clear();

        borderPaneRoot.setOpacity(0.3);

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
        contentPane.getChildren().setAll(aboutController.getRoot());
    }

    @FXML
    void onConnectAction(ActionEvent event) {
        contentPane.getChildren().setAll(connectController.getRoot());
    }

    @FXML
    void onContactAction(ActionEvent event) {
        contentPane.getChildren().setAll(contactController.getRoot());
    }

    @FXML
    void onLogsAction(ActionEvent event) {
        // load log table
        LogTable logTable = new LogTable();

        // Bind the logs from the Command class to the one in the log table
        logTable.getLogs().bind(connectController.getBot().getCommands().getLogs());
        contentPane.getChildren().setAll(logTable.getLogRoot());
    }

    @FXML
    void onSettingsAction(ActionEvent event) {
        contentPane.getChildren().setAll(settingsController.getRoot());
    }

    @FXML
    void onChangeTheme(ActionEvent event) {
        String lightTheme = getClass().getResource("/styles/light-theme.css").toExternalForm();
        String darkTheme = getClass().getResource("/styles/dark-theme.css").toExternalForm();

        String currentTheme = Utils.properties.getProperty("theme");
        System.out.println("Current theme: " + currentTheme); // Debugging line

        if (currentTheme.equals("dark")) {
            Application.setUserAgentStylesheet(lightTheme);
            Utils.setTheme("light");
            System.out.println("Theme changed to light"); // Debugging line
        } else {
            Application.setUserAgentStylesheet(darkTheme);
            Utils.setTheme("dark");
            System.out.println("Theme changed to dark"); // Debugging line
        }
    }

    private void stopTutorial() {
        tutorialPopOvers.forEach((key, value) -> value.hide());
        tutorialStep = 0;
        tutorialPane.setVisible(false);
        borderPaneRoot.setOpacity(1);
    }

    // getters and setters
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

    public Button getSettingsButton() {
        return settingsButton;
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