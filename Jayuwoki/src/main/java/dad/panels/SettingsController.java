package dad.panels;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import java.io.File;

public class SettingsController implements Initializable {

    @FXML
    private Label botSettingsLabel;

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
        saveSettings();

        // if any changes are made, it will show a dialog

        Dialog savedDialog = new Dialog();
        savedDialog.setContentText("Settings saved successfully!");
        savedDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        savedDialog.showAndWait();
    }

    @FXML
    void onResetAction(ActionEvent event) {

        // show a dialog to confirm the reset
        Dialog resetDialog = new Dialog();
        resetDialog.setContentText("Are you sure you want to reset all settings?");
        resetDialog.getDialogPane().getButtonTypes().add(ButtonType.YES);
        resetDialog.getDialogPane().getButtonTypes().add(ButtonType.NO);
        if (resetDialog.showAndWait().get() == ButtonType.YES) {
            clearCheckBox();
            saveSettings();
        }
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
    }

    public BorderPane getRoot() {
        return settingsRoot;
    }
}