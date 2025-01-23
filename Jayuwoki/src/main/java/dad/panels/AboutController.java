package dad.panels;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML
    private WebView webRoot;

    public AboutController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AboutView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webRoot = new WebView();
        URL webPageUrl = getClass().getResource("/webpage/about.html");
        if (webPageUrl != null) {
            webRoot.getEngine().load(webPageUrl.toExternalForm());
        } else {
            System.err.println("Error: No se pudo encontrar el archivo about.html");
        }
    }

    public WebView getRoot() {
        return webRoot;
    }
}