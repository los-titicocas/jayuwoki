package dad.panels;

import dad.api.models.LogEntry;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import net.dv8tion.jda.api.entities.User;

import java.net.URL;
import java.util.ResourceBundle;

public class LogTable implements Initializable {

    private ListProperty<LogEntry> logs = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final ObjectProperty<LogEntry> selectedLog = new SimpleObjectProperty<>();

    public ListProperty<LogEntry> getLogs() {
        return logs;
    }


    public void setLogs(ListProperty<LogEntry> logs) {
        this.logs = logs;
    }

    @FXML
    private Button clearButton;

    @FXML
    private TableView<LogEntry> logsTable;

    @FXML
    private TableColumn<LogEntry, String> userColumn;

    @FXML
    private TableColumn<LogEntry, String> promptColumn;

    @FXML
    private TableColumn<LogEntry, String> dateColumn;

    @FXML
    private BorderPane logRoot;

    @FXML
    void onClearAction(ActionEvent event) {
        // Confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmar eliminación");
        confirmAlert.setHeaderText("¿Estás seguro de que deseas eliminar este registro?");
        confirmAlert.setContentText("No se podrá recuperar");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Remove the selected log if user confirms
                logs.remove(selectedLog.get());
            }
        });
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
        userColumn.setCellValueFactory(cellData -> cellData.getValue().userProperty());
        promptColumn.setCellValueFactory(cellData -> cellData.getValue().promptProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());

        // bind logs to logsTable
        logsTable.itemsProperty().bindBidirectional(logs);

        // bind clearButton to logsTable selection
        clearButton.disableProperty().bind(logsTable.getSelectionModel().selectedItemProperty().isNull());

        // bind selectedLog to logsTable selection
        selectedLog.bind(logsTable.getSelectionModel().selectedItemProperty());
    }

    public TableView<LogEntry> getLogsTable() {
        return logsTable;
    }

    public BorderPane getLogRoot() {
        return logRoot;
    }
}
