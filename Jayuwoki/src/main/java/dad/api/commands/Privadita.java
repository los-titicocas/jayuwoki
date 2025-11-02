package dad.api.commands;

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

  //TODO:
  private MessageReceivedEvent event;
  private ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());
  private StringProperty server = new SimpleStringProperty();
  private final ArrayList<String> roles = new ArrayList<>(List.of("Top", "Jungla", "Mid", "ADC", "Support"));

  public Privadita(List<Player> players, MessageReceivedEvent event) {
    this.server.set(event.getGuild().getName());
    CheckPrivaditaCommand(players,event);
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


  // Function to check the command and fill the player list
  private ListProperty<Player> CheckPrivaditaCommand(List<Player> playerList, MessageReceivedEvent event) {
    Set<String> uniqueNames = new HashSet<>();
    ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());

    for (Player player : playerList) {
      String name = player.getName();

      if (!uniqueNames.add(name)) { // Si el nombre ya existe en el conjunto
        event.getChannel().sendMessage(name + " está repetido, prueba otra vez.").queue();
        return null;
      }

      if (name.startsWith("$")) {
        event.getChannel().sendMessage("A donde vas listillo.").queue();
        return null;
      }

      players.add(player);
    }
    return players;
  }

  public void ResultadoPrivadita(String ganador, MessageReceivedEvent event) {
    System.out.println("Elo de todos los jugadores:");
    for (Player p : players) {
      System.out.println(p.getName() + " - Elo: " + p.getElo());
    }

    double averageEloEquipo1 = players.subList(0, 5).stream()
            .mapToInt(Player::getElo)
            .average()
            .orElse(0);

    double averageEloEquipo2 = players.subList(5, 10).stream()
            .mapToInt(Player::getElo)
            .average()
            .orElse(0);

    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Resultado de la partida:\n");

    switch (ganador) {
      case "blue":
        messageBuilder.append("\n🏆 **Equipo Azul ha ganado!** 🏆\n");
        break;
      case "red":
        messageBuilder.append("\n🔥 **Equipo Rojo ha ganado!** 🔥\n");
        break;
      default:
        event.getChannel().sendMessage("Pon el equipo bien tolete").queue();
        return;
    }

    messageBuilder.append("```\n**Cambios de Elo:**\n");

    for (int i = 0; i < 5; i++) {
      if (ganador.equals("blue")) {
        players.get(i).setWins(players.get(i).getWins() + 1);
        players.get(i + 5).setLosses(players.get(i + 5).getLosses() + 1);
        int oldEloWin = players.get(i).getElo();
        int oldEloLose = players.get(i + 5).getElo();
        players.get(i).ActualizarElo(averageEloEquipo2, true);
        players.get(i + 5).ActualizarElo(averageEloEquipo1, false);

        messageBuilder.append(players.get(i).getName()).append(": ")
                .append(oldEloWin).append(" ➝ ").append(players.get(i).getElo())
                .append(" (+").append(players.get(i).getElo() - oldEloWin).append(")\n");

        messageBuilder.append(players.get(i + 5).getName()).append(": ")
                .append(oldEloLose).append(" ➝ ").append(players.get(i + 5).getElo())
                .append(" (").append(players.get(i + 5).getElo() - oldEloLose).append(")\n");
      } else {
        players.get(i + 5).setWins(players.get(i + 5).getWins() + 1);
        players.get(i).setLosses(players.get(i).getLosses() + 1);
        int oldEloWin = players.get(i + 5).getElo();
        int oldEloLose = players.get(i).getElo();
        players.get(i + 5).ActualizarElo(averageEloEquipo1, true);
        players.get(i).ActualizarElo(averageEloEquipo2, false);

        messageBuilder.append(players.get(i + 5).getName()).append(": ")
                .append(oldEloWin).append(" ➝ ").append(players.get(i + 5).getElo())
                .append(" (+").append(players.get(i + 5).getElo() - oldEloWin).append(")\n");

        messageBuilder.append(players.get(i).getName()).append(": ")
                .append(oldEloLose).append(" ➝ ").append(players.get(i).getElo())
                .append(" (").append(players.get(i).getElo() - oldEloLose).append(")\n");
      }
    }

    messageBuilder.append("```");
    event.getChannel().sendMessage(messageBuilder.toString()).queue();
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
