package dad.custom.ui;

import dad.api.Bot;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;
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
    private Button closeButton;

    @FXML
    private GridPane customTitleBar;

    @FXML
    private Button maximizeButton;

    @FXML
    private Button minimizeButton;

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

        customTitleBar.setOnMousePressed(event -> {
            Stage stage = (Stage) customTitleBar.getScene().getWindow();
            xOffset = stage.getX() - event.getScreenX();
            yOffset = stage.getY() - event.getScreenY();
        });

        customTitleBar.setOnMouseDragged(event -> {
            Stage stage = (Stage) customTitleBar.getScene().getWindow();
            stage.setX(event.getScreenX() + xOffset);
            stage.setY(event.getScreenY() + yOffset);
        });
        
    }

    @FXML
    private void onMinimizeWindow() {
        Stage stage = (Stage) customTitleBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void onMaximizeWindow() {
        Stage stage = (Stage) customTitleBar.getScene().getWindow();

        // get screen size
        Screen screen = Screen.getPrimary();

        Rectangle2D visualBounds = screen.getVisualBounds();

        // set stage to screen size depending on the maximized state
        if (stage.getX() == visualBounds.getMinX() && stage.getY() == visualBounds.getMinY() && stage.getWidth() == visualBounds.getWidth() && stage.getHeight() == visualBounds.getHeight()) {
            stage.setWidth(1000);
            stage.setHeight(600);
            stage.centerOnScreen();
        } else {
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
        }
    }

    @FXML
    public void onCloseWindow() {
        Stage stage = (Stage) customTitleBar.getScene().getWindow();
        stage.close();

        if (bot != null) {
            bot.stopConnection();
        }
    }

    public GridPane getRoot() {
        return customTitleBar;
    }
}

