package dad.panels;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

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
    }

    @FXML
    void onApplyAction(ActionEvent event) {

    }

    @FXML
    void onResetAction(ActionEvent event) {

    }

    public BorderPane getRoot() {
        return settingsRoot;
    }
}