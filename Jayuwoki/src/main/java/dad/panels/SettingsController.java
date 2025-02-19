package dad.panels;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import java.io.File;

public class SettingsController implements Initializable {

    private final Image appIcon = new Image(Objects.requireNonNull(getClass().getResource("/images/logo.png")).toString());

    @FXML
    private Label botSettingsLabel;

    @FXML
    private FontIcon firebaseIcon;

    @FXML
    private Button dbFileButton;

    @FXML
    private Label userSettingsLabel;

    @FXML
    private GridPane settingsGridPane;

    @FXML
    private Button applyButton;

    @FXML
    private CheckBox imageCheck;

    @FXML
    private CheckBox massPermissionCheck;

    @FXML
    private BorderPane settingsRoot;

    @FXML
    private CheckBox soundCheck;

    @FXML
    private CheckBox rolladieCheck;

    public SettingsController() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SettingsView.fxml"));
        fxmlLoader.setController(this);
        try {
            settingsRoot = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSettings();
    }

    @FXML
    void onApplyAction(ActionEvent event) {
        getApplyDialog();
        saveSettings();
    }

    @FXML
    void onResetAction(ActionEvent event) {
        getResetDialog();
    }

    @FXML
    void onDbFileAction(ActionEvent event) {

    }

    private void getResetDialog() {
        // show a dialog to confirm the reset
        Alert resetDialog = new Alert(Alert.AlertType.CONFIRMATION);

        resetDialog.setTitle("Reset Settings");
        resetDialog.setHeaderText(null);
        resetDialog.setContentText("Are you sure you want to reset all settings?");

        ButtonType confirmButton = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        resetDialog.getButtonTypes().setAll(confirmButton, cancelButton);

        // set icon for the dialog
        Stage stage = (Stage) resetDialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(appIcon);

        if (resetDialog.showAndWait().get() == confirmButton) {
            clearCheckBox();
            saveSettings();
        }
    }

    private void getApplyDialog() {
        Alert applyDialog = new Alert(Alert.AlertType.INFORMATION);

        applyDialog.setTitle("Settings Applied");
        applyDialog.setHeaderText(null);
        applyDialog.setContentText("Settings have been applied successfully. Please restart the application for changes to take effect.");

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        applyDialog.getButtonTypes().setAll(okButton);

        // set icon for the dialog
        Stage stage = (Stage) applyDialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(appIcon);

        applyDialog.showAndWait();
    }

    private void loadSettings() {
        File settingsFile = new File("Jayuwoki/settings.properties");
        Properties properties = new Properties();

        if (settingsFile.exists()) {
            try (FileInputStream input = new FileInputStream(settingsFile)) {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        imageCheck.setSelected(Boolean.parseBoolean(properties.getProperty("imageCheck", "false")));
        massPermissionCheck.setSelected(Boolean.parseBoolean(properties.getProperty("massPermissionCheck", "false")));
        soundCheck.setSelected(Boolean.parseBoolean(properties.getProperty("soundCheck", "false")));
        rolladieCheck.setSelected(Boolean.parseBoolean(properties.getProperty("rollaDie", "true")));
    }

    private void saveSettings() {
        File settingsFile = new File("Jayuwoki/settings.properties");
        Properties properties = new Properties();

        if (settingsFile.exists()) {
            try (FileInputStream input = new FileInputStream(settingsFile)) {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        properties.setProperty("imageCheck", String.valueOf(imageCheck.isSelected()));
        properties.setProperty("massPermissionCheck", String.valueOf(massPermissionCheck.isSelected()));
        properties.setProperty("soundCheck", String.valueOf(soundCheck.isSelected()));
        properties.setProperty("rollaDie", String.valueOf(rolladieCheck.isSelected()));

        try (FileOutputStream output = new FileOutputStream(settingsFile)) {
            properties.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearCheckBox() {
        imageCheck.setSelected(false);
        massPermissionCheck.setSelected(false);
        soundCheck.setSelected(false);
        rolladieCheck.setSelected(true);
    }

    public BorderPane getRoot() {
        return settingsRoot;
    }

}