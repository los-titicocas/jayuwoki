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

  // Attributes
  private MessageReceivedEvent event;
  private ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());
  private StringProperty server = new SimpleStringProperty();
  private final ArrayList<String> roles = new ArrayList<>(List.of("Top", "Jungla", "Mid", "ADC", "Support"));
  // true solo si se validaron los jugadores y se hicieron los equipos (10 jugadores: 0-4 Blue, 5-9 Red)
  private boolean valid = false;

  /** Equipo ganador de una privadita. */
  public enum Team {
    BLUE, RED;

    /**
     * Normaliza la entrada del usuario: ignora espacios y mayúsculas/minúsculas
     * ("red", "RED", " Blue ", "bLuE"...). Devuelve vacío si no es un equipo válido (o es null).
     */
    public static Optional<Team> parse(String input) {
      if (input == null) {
        return Optional.empty();
      }
      switch (input.trim().toLowerCase(Locale.ROOT)) {
        case "blue":
          return Optional.of(BLUE);
        case "red":
          return Optional.of(RED);
        default:
          return Optional.empty();
      }
    }
  }

  /** Copia de elo/wins/losses de un jugador, para poder deshacer un resultado si falla el guardado. */
  public record StatsSnapshot(Player player, int elo, int wins, int losses) {
  }

  // Helper class to store team split
  private static class TeamSplit {
    List<Player> blueTeam;
    List<Player> redTeam;

    TeamSplit(List<Player> blueTeam, List<Player> redTeam) {
      this.blueTeam = blueTeam;
      this.redTeam = redTeam;
    }
  }

  public Privadita(List<Player> players, MessageReceivedEvent event) {
    this.server.set(event.getGuild().getName());
    this.event = event;

    // Validate and fill this.players
    if (!CheckPrivaditaCommand(players, event)) {
      // Invalid command or players, do not start privadita
      return;
    }

    // Start the match setup
    StartPrivadita(event);
  }

  /** Solo para tests: privadita ya formada (0-4 Blue, 5-9 Red), sin evento de Discord. */
  Privadita(List<Player> orderedPlayers) {
    this.players.addAll(orderedPlayers);
    this.valid = orderedPlayers.size() == 10;
  }

  // java
  private void StartPrivadita(MessageReceivedEvent event) {
    Random rand = new Random();
    int privaditaEspecial = rand.nextInt(21); // 0..20
    System.out.println("Número aleatorio para privadita especial: " + privaditaEspecial);

    ObservableList<Player> playerList = this.players.get();

    if (playerList == null || playerList.size() != 10) {
      event.getChannel().sendMessage("Necesito exactamente 10 jugadores para hacer equipos balanceados.").queue();
      return;
    }

    List<Player> baseList = new ArrayList<>(playerList);
    TeamSplit split = createBalancedTeams(baseList);
    List<Player> blueTeam = split.blueTeam;
    List<Player> redTeam = split.redTeam;

    Collections.shuffle(blueTeam);
    Collections.shuffle(redTeam);

    // Prepara roles locales sin tocar `this.roles`
    List<String> baseRoles = new ArrayList<>(roles); // copia segura
    int roleCount = baseRoles.size(); // esperado 5

    List<String> blueRoles = new ArrayList<>(Collections.nCopies(5, ""));
    List<String> redRoles = new ArrayList<>(Collections.nCopies(5, ""));

    if (privaditaEspecial == 20) {
      // Caso especial: no asignar roles explícitos
      for (int i = 0; i < 5; i++) {
        blueRoles.set(i, "");
        redRoles.set(i, "");
      }
      // print special message to chat
        event.getChannel().sendMessage("\uD83D\uDD25 Freemolly labubu ayiyi ahora no tienen excusa").queue();
    } else if (privaditaEspecial >= 15) {
      // 15..19: un jugador por equipo recibe '*' en su rol
      int blueSpecialIndex = rand.nextInt(5);
      int redSpecialIndex = rand.nextInt(5);
      for (int i = 0; i < 5; i++) {
        String base = baseRoles.get(i % roleCount);
        blueRoles.set(i, (i == blueSpecialIndex) ? base + "*" : base);
        redRoles.set(i, (i == redSpecialIndex) ? base + "*" : base);
      }
      //print to chat who are the special players
        event.getChannel().sendMessage("\uD83D\uDD25 Atención: " + blueTeam.get(blueSpecialIndex).getName() + " y " + redTeam.get(redSpecialIndex).getName() + " pueden cambiar su rol con cualquier miembro del equipo (*)").queue();

    } else {
      // Caso normal: roles por defecto
      for (int i = 0; i < 5; i++) {
        String base = baseRoles.get(i % roleCount);
        blueRoles.set(i, base);
        redRoles.set(i, base);
      }
    }

    // Asignar roles finales (una sola vez)
    for (int i = 0; i < 5; i++) {
      blueTeam.get(i).setRole(blueRoles.get(i));
      redTeam.get(i).setRole(redRoles.get(i));
    }

    // Rebuild internal players list (0-4 Blue, 5-9 Red)
    ObservableList<Player> orderedPlayers = FXCollections.observableArrayList();
    orderedPlayers.addAll(blueTeam);
    orderedPlayers.addAll(redTeam);
    this.players.set(orderedPlayers);

    // Calcular promedios y enviar mensaje (igual que antes)
    int blueTotalElo = blueTeam.stream().mapToInt(Player::getElo).sum();
    int redTotalElo = redTeam.stream().mapToInt(Player::getElo).sum();
    double blueAvgElo = blueTotalElo / (double) blueTeam.size();
    double redAvgElo = redTotalElo / (double) redTeam.size();

    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("```")
            .append("\nBlue Team (avg Elo: ")
            .append(String.format(java.util.Locale.US, "%.1f", blueAvgElo))
            .append(")\n");

    for (int i = 0; i < 5; i++) {
      Player player = this.players.get(i);
      messageBuilder.append(player.getName())
              .append(" (Elo: ").append(player.getElo()).append(")")
              .append(" -> ")
              .append(player.getRole())
              .append("\n");
    }

    messageBuilder.append("\nRed Team (avg Elo: ")
            .append(String.format(java.util.Locale.US, "%.1f", redAvgElo))
            .append(")\n");

    for (int i = 5; i < 10; i++) {
      Player player = this.players.get(i);
      messageBuilder.append(player.getName())
              .append(" (Elo: ").append(player.getElo()).append(")")
              .append(" -> ")
              .append(player.getRole())
              .append("\n");
    }

    messageBuilder.append("```");
    event.getChannel().sendMessage(messageBuilder.toString()).queue();
    valid = true;
  }


  // Brute-force optimal split by Elo
  private TeamSplit createBalancedTeams(List<Player> playerList) {
    int n = playerList.size(); // should be 10
    int totalElo = playerList.stream().mapToInt(Player::getElo).sum();

    int bestMask = 0;
    int bestDiff = Integer.MAX_VALUE;

    int limit = 1 << n; // 2^n

    for (int mask = 0; mask < limit; mask++) {
      // We only want teams of size n/2 (5 players)
      if (Integer.bitCount(mask) != n / 2) {
        continue;
      }

      int eloTeamA = 0;
      for (int i = 0; i < n; i++) {
        if ((mask & (1 << i)) != 0) {
          eloTeamA += playerList.get(i).getElo();
        }
      }

      int eloTeamB = totalElo - eloTeamA;
      int diff = Math.abs(eloTeamA - eloTeamB);

      if (diff < bestDiff) {
        bestDiff = diff;
        bestMask = mask;

        // Early exit if perfect balance
        if (diff == 0) {
          break;
        }
      }
    }

    List<Player> blueTeam = new ArrayList<>();
    List<Player> redTeam = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      if ((bestMask & (1 << i)) != 0) {
        blueTeam.add(playerList.get(i));
      } else {
        redTeam.add(playerList.get(i));
      }
    }

    return new TeamSplit(blueTeam, redTeam);
  }

  // Function to check the command and fill the player list
  private boolean CheckPrivaditaCommand(List<Player> playerList, MessageReceivedEvent event) {
    Set<String> uniqueNames = new HashSet<>();

    for (Player player : playerList) {
      String name = player.getName();

      // Check duplicated names
      if (!uniqueNames.add(name)) {
        event.getChannel().sendMessage(name + " está repetido, prueba otra vez.").queue();
        return false;
      }

      // Check trolling names
      if (name.startsWith("$")) {
        event.getChannel().sendMessage("A donde vas listillo.").queue();
        return false;
      }

      this.players.add(player);
    }

    return true;
  }

  /**
   * Versión original del comando (valida, aplica y envía el mensaje). Se mantiene por compatibilidad;
   * Commands usa ahora {@link Team#parse(String)} + {@link #aplicarResultado(Team)} para poder guardar en
   * la base de datos antes de anunciar y deshacer si algo falla.
   */
  public void ResultadoPrivadita(String ganador, MessageReceivedEvent event) {
    Optional<Team> team = Team.parse(ganador);
    if (team.isEmpty()) {
      event.getChannel().sendMessage("Pon el equipo bien tolete").queue();
      return;
    }
    event.getChannel().sendMessage(aplicarResultado(team.get())).queue();
  }

  /**
   * Aplica el resultado: suma victorias/derrotas y actualiza el Elo de los 10 jugadores.
   * Primero valida y calcula todos los Elo nuevos y SOLO después modifica a los jugadores, para que un error
   * nunca deje la privadita a medias (con unos jugadores actualizados y otros no).
   *
   * @return el mensaje con los cambios de Elo para enviar al chat
   * @throws IllegalStateException si la privadita no tiene 10 jugadores válidos
   */
  public String aplicarResultado(Team ganador) {
    Objects.requireNonNull(ganador, "ganador");
    List<Player> lista = players.get() == null ? List.of() : new ArrayList<>(players.get());
    if (lista.size() != 10 || lista.contains(null)) {
      throw new IllegalStateException("La privadita no tiene 10 jugadores válidos (tiene " + lista.size() + ")");
    }

    System.out.println("Elo de todos los jugadores:");
    for (Player p : lista) {
      System.out.println(p.getName() + " - Elo: " + p.getElo());
    }

    double averageEloEquipo1 = lista.subList(0, 5).stream().mapToInt(Player::getElo).average().orElse(0); // Blue
    double averageEloEquipo2 = lista.subList(5, 10).stream().mapToInt(Player::getElo).average().orElse(0); // Red

    boolean blueGana = ganador == Team.BLUE;
    List<Player> ganadores = blueGana ? lista.subList(0, 5) : lista.subList(5, 10);
    List<Player> perdedores = blueGana ? lista.subList(5, 10) : lista.subList(0, 5);
    double eloRivalDeGanadores = blueGana ? averageEloEquipo2 : averageEloEquipo1;
    double eloRivalDePerdedores = blueGana ? averageEloEquipo1 : averageEloEquipo2;

    // 1) Calcular sin tocar a los jugadores (misma fórmula que Player.ActualizarElo)
    int[] nuevoEloGanador = new int[5];
    int[] nuevoEloPerdedor = new int[5];
    for (int i = 0; i < 5; i++) {
      nuevoEloGanador[i] = eloTrasPartida(ganadores.get(i).getElo(), eloRivalDeGanadores, true);
      nuevoEloPerdedor[i] = eloTrasPartida(perdedores.get(i).getElo(), eloRivalDePerdedores, false);
    }

    // 2) Aplicar y construir el mensaje (mismo formato que antes)
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Resultado de la partida:\n");
    messageBuilder.append(blueGana
            ? "\n🏆 **Equipo Azul ha ganado!** 🏆\n"
            : "\n🔥 **Equipo Rojo ha ganado!** 🔥\n");
    messageBuilder.append("```\n**Cambios de Elo:**\n");

    for (int i = 0; i < 5; i++) {
      Player ganadorI = ganadores.get(i);
      Player perdedorI = perdedores.get(i);
      int oldEloWin = ganadorI.getElo();
      int oldEloLose = perdedorI.getElo();

      ganadorI.setWins(ganadorI.getWins() + 1);
      ganadorI.setElo(nuevoEloGanador[i]);
      perdedorI.setLosses(perdedorI.getLosses() + 1);
      perdedorI.setElo(nuevoEloPerdedor[i]);

      messageBuilder.append(ganadorI.getName()).append(": ")
              .append(oldEloWin).append(" ➝ ").append(ganadorI.getElo())
              .append(" (+").append(ganadorI.getElo() - oldEloWin).append(")\n");

      messageBuilder.append(perdedorI.getName()).append(": ")
              .append(oldEloLose).append(" ➝ ").append(perdedorI.getElo())
              .append(" (").append(perdedorI.getElo() - oldEloLose).append(")\n");
    }

    messageBuilder.append("```");
    return messageBuilder.toString();
  }

  private static int eloTrasPartida(int elo, double eloMedioRival, boolean gana) {
    Player copia = new Player();
    copia.setElo(elo);
    copia.ActualizarElo(eloMedioRival, gana);
    return copia.getElo();
  }

  /** Guarda elo/wins/losses actuales de los jugadores (para deshacer con {@link #restoreStats}). */
  public List<StatsSnapshot> snapshotStats() {
    List<StatsSnapshot> snapshot = new ArrayList<>();
    if (players.get() != null) {
      for (Player p : players.get()) {
        if (p != null) {
          snapshot.add(new StatsSnapshot(p, p.getElo(), p.getWins(), p.getLosses()));
        }
      }
    }
    return snapshot;
  }

  public static void restoreStats(List<StatsSnapshot> snapshot) {
    for (StatsSnapshot s : snapshot) {
      s.player().setElo(s.elo());
      s.player().setWins(s.wins());
      s.player().setLosses(s.losses());
    }
  }

  /** false si el comando $privadita no pasó la validación (nombres repetidos, etc.). */
  public boolean isValid() {
    return valid;
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
