package dad.api.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {

  private final StringProperty user = new SimpleStringProperty();
  private final StringProperty date = new SimpleStringProperty();
  private final StringProperty prompt = new SimpleStringProperty();
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

  public String getUser() {
    return user.get();
  }

  public StringProperty userProperty() {
    return user;
  }

  public void setUser(String user) {
    this.user.set(user);
  }

  public String getDate() {
    return date.get();
  }

  public StringProperty dateProperty() {
    return date;
  }

  public void setDate(LocalDateTime date) {
    this.date.set(date.format(formatter));
  }

  public String getPrompt() {
    return prompt.get();
  }

  public StringProperty promptProperty() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt.set(prompt);
  }

  public LogEntry() {
  }

  public LogEntry(String user, String prompt, LocalDateTime date) {
    this.user.set(user);
    this.date.set(date.format(formatter));
    this.prompt.set(prompt);
  }
}