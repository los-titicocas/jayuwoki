package dad.panels;

import com.vladsch.flexmark.ext.typographic.internal.SmartsInlineParser;
import dad.api.Bot;
import dad.controllers.MainController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;


public class ConnectController implements Initializable {

    private MainController mainController;
    private final Bot bot = new Bot();

    public Bot getBot() {
        return bot;
    }

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private GridPane connectRoot;

    public ConnectController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConnectView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // disable connect button if bot is already connected
        connectButton.disableProperty().bind(bot.isconnectedProperty());

        // disable disconnect button if bot is not connected
        disconnectButton.disableProperty().bind(bot.isconnectedProperty().not());
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void onDisconnectAction(ActionEvent event) {
        try {
            bot.stopConnection();
            // enable settings button if bot is disconnected
            mainController.getSettingsButton().setDisable(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onConnectAction(ActionEvent event) {
        try {
            bot.startConnection();
            // disable settings button if bot is connected
            mainController.getSettingsButton().setDisable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public GridPane getRoot() {
        return connectRoot;
    }
}
