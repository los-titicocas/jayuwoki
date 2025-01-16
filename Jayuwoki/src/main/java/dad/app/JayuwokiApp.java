package dad.app;

import dad.controllers.LoginController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class JayuwokiApp extends Application {

    LoginController loginController = new LoginController();

    @Override
    public void start(Stage primaryStage) throws Exception {

//        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        String darkTheme = getClass().getResource("/styles/dark-theme.css").toExternalForm();
        Application.setUserAgentStylesheet(darkTheme);
        Image appIcon = new Image(Objects.requireNonNull(getClass().getResource("/images/logo.png")).toString());

        Scene scene = new Scene(loginController.getRoot());
        Stage stage = new Stage();

        stage.getIcons().add(appIcon);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
    }
}
