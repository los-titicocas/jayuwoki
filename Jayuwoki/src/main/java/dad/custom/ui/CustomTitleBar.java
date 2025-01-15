package dad.custom.ui;

import dad.api.Bot;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class CustomTitleBar implements Initializable {

    private double xOffset = 0;
    private double yOffset = 0;

    private Bot bot;

    public void setBot(Bot bot) {
        this.bot = bot;
    }

    @FXML
    private GridPane root;

    public CustomTitleBar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomTitleBarView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        root.setOnMousePressed(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        root.setOnMouseDragged(event -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
    }

    @FXML
    private void onMinimizeWindow() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void onMaximizeWindow() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void onCloseWindow() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();

        bot.stopConnection();

    }

    public GridPane getRoot() {
        return root;
    }
}

