package dad.panels;

import dad.api.models.LogEntry;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.dv8tion.jda.api.entities.User;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class LogTable implements Initializable {

    private final Image appIcon = new Image(Objects.requireNonNull(getClass().getResource("/images/logo.png")).toString());

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

        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete this register?");

        ButtonType acceptButton = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(acceptButton, cancelButton);

        Stage stage = (Stage) confirmAlert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(appIcon);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == acceptButton) {
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

        // Add a row factory to handle empty row styling
        logsTable.setRowFactory(tv -> {
            TableRow<LogEntry> row = new TableRow<>();
            PseudoClass emptyPseudoClass = PseudoClass.getPseudoClass("empty");

            // Update the pseudo-class based on the row's data
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                boolean isEmpty = newItem == null;
                row.pseudoClassStateChanged(emptyPseudoClass, isEmpty);
            });

            return row;
        });

    }

    public TableView<LogEntry> getLogsTable() {
        return logsTable;
    }

    public BorderPane getLogRoot() {
        return logRoot;
    }
}
