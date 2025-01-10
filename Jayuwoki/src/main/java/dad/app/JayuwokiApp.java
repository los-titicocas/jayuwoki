package dad.app;

import atlantafx.base.theme.Dracula;
import dad.controllers.LoginController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JayuwokiApp extends Application {

    LoginController loginController = new LoginController();

    @Override
    public void start(Stage primaryStage) throws Exception {

        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());

        Scene scene = new Scene(loginController.getRoot());
        Stage stage = new Stage();
        stage.setTitle("JayuwokiBot");
        stage.setScene(scene);
        stage.show();
    }
}
