package dad.panels;

import dad.api.Bot;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;


public class ConnectController implements Initializable {

    private final Bot bot = new Bot();

    public Bot getBot() {
        return bot;
    }

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private GridPane root;

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

    @FXML
    void onDisconnectAction(ActionEvent event) {
        try {
            bot.stopConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onConnectAction(ActionEvent event) {
        try {
            bot.startConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public GridPane getRoot() {
        return root;
    }
}
