package dad.api.models;

import dad.database.Player;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;

public class Privadita {

  // Atributtes
  private MessageReceivedEvent event;
  private ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());
  private StringProperty server = new SimpleStringProperty();
  private final ArrayList<String> roles = new ArrayList<>(List.of("Top", "Jungla", "Mid", "ADC", "Support"));

  public Privadita(List<Player> players, MessageReceivedEvent event) {
    this.server.set(event.getGuild().getName());
    this.players.addAll(players);
    StartPrivadita(event);
  }

  private void StartPrivadita(MessageReceivedEvent event) {

    // Shuffle the players so the teams are random
    Collections.shuffle(players);

    // Divide the players in two teams
    for (int i = 0; i < 10; i++) {
      players.get(i).setRole(roles.get(i % 5));
    }
    // Create the message with the 2 teams
    StringBuilder messageBuilder = new StringBuilder();

    messageBuilder.append("```")
            .append("\nBlue Team\n");

    // Add the players to the blue team first 5
    for (int i = 0; i < 5; i++) {
      Player player = players.get(i);
      messageBuilder.append(player.getName())
              .append(" -> ")
              .append(player.getRole())
              .append("\n");
    }

    messageBuilder.append("\nRed Team\n");

// Añadir los siguientes 5 jugadores al equipo rojo
    for (int i = 5; i < 10; i++) {
      Player player = players.get(i);
      messageBuilder.append(player.getName())
              .append(" -> ")
              .append(player.getRole())
              .append("\n");
    }

    messageBuilder.append("```");


    String formattedMessage = messageBuilder.toString();
    event.getChannel().sendMessage(formattedMessage).queue();
  }

  // FUnction to check the command and fill the player list
  private ListProperty<Player> CheckPrivaditaCommand(List<String> playersNames, MessageReceivedEvent event) {
    Set<String> uniqueNames = new HashSet<>();

    for (String name : playersNames) {
      if (!uniqueNames.add(name)) { // Si el nombre ya existe en el conjunto
        event.getChannel().sendMessage(name + " está repetido, prueba otra vez.").queue();
        return null;
      }

      if (name.startsWith("$")) {
        event.getChannel().sendMessage("A donde vas listillo.").queue();
        return null;
      }
      Player player = new Player();
      player.setName(name);
      players.add(player);
    }
    return players;
  }

  public MessageReceivedEvent getEvent() {
    return event;
  }

  public void setEvent(MessageReceivedEvent event) {
    this.event = event;
  }

  public ObservableList<Player> getPlayers() {
    return players.get();
  }

  public ListProperty<Player> playersProperty() {
    return players;
  }

  public void setPlayers(ObservableList<Player> players) {
    this.players.set(players);
  }

  public String getServer() {
    return server.get();
  }

  public StringProperty serverProperty() {
    return server;
  }

  public void setServer(String server) {
    this.server.set(server);
  }

  public ArrayList<String> getRoles() {
    return roles;
  }
}
