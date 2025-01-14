package dad.panels;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class LogTable implements Initializable {

    @FXML
    private Button clearButton;

    @FXML
    private TableColumn<?, ?> dateColumn;

    @FXML
    private TableView<?> logsTable;

    @FXML
    private TableColumn<?, ?> promptColumn;

    @FXML
    private BorderPane root;

    @FXML
    private TableColumn<?, ?> userColumn;

    @FXML
    void onClearAction(ActionEvent event) {

    }

    @FXML
    void onRefreshAction(ActionEvent event) {

    }

    public LogTable() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LogTableView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // initialize log columns
        userColumn.setCellValueFactory(null);
        promptColumn.setCellValueFactory(null);
        dateColumn.setCellValueFactory(null);

        // bind clearButton to logsTable selection
        clearButton.disableProperty().bind(logsTable.getSelectionModel().selectedItemProperty().isNull());
    }

    public TableView<?> getLogsTable() {
        return logsTable;
    }

    public BorderPane getRoot() {
        return root;
    }
}
