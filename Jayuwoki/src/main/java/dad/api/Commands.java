package dad.api;

import dad.api.models.LogEntry;
import dad.database.Player;
import dad.database.DBManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;


public class Commands extends ListenerAdapter {

    private final ArrayList<String> roles = new ArrayList<>(List.of("Top", "Jungla", "Mid", "ADC", "Support"));
    private ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ListProperty<LogEntry> logs = new SimpleListProperty<>(FXCollections.observableArrayList());
    private BooleanProperty activeGame = new SimpleBooleanProperty();
    private final DBManager dbManager = new DBManager();
    private JDA jda;

    public Commands() {
        activeGame.set(false);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        System.out.println("imprime coño: " + event.getGuild().getName());
        String message = event.getMessage().getContentRaw();
        if (message.startsWith("$")) {
            dbManager.setEvent(event);
            dbManager.setCurrentServer(event.getGuild().getName());
            String[] comando = message.split(" ");

            // Introduce the command in the log
            LogEntry logEntry = new LogEntry(event.getAuthor().getName(), message, event.getMessage().getTimeCreated().toLocalDateTime());
            logs.add(logEntry);

            // Switch with all the possible commands
            switch (comando[0]) {
                // Start a privadita
                case "$privadita":
                    // Check if the command has the correct number of players
                    if (activeGame.get()) {
                        event.getChannel().sendMessage("Ya hay una privadita en juego ACÁBALA (o $dropPrivadita)").queue();
                    } else if (comando.length == 11) {
                        StartPrivadita(comando, event);
                    } else {
                        event.getChannel().sendMessage("El comando $privadita necesita 10 jugadores").queue();
                    }
                    break;

                // Remove the current privadita
                case "$dropPrivadita":
                    if (activeGame.get()) {
                        activeGame.set(false);
                        players.clear();
                        event.getChannel().sendMessage("Se ha cancelado la privadita").queue();
                    } else {
                        event.getChannel().sendMessage("No hay ninguna privadita activa").queue();
                    }
                    break;

                // Add only one player to the database to use it on privadita
                case "$addPlayer":
                    if (comando.length == 2) {
                        Player newPlayer = new Player();
                        newPlayer.setName(comando[1]);
                        dbManager.AddPlayer(newPlayer);
                    } else {
                        event.getChannel().sendMessage("El comando $addPlayer necesita un nombre de jugador").queue();
                    }
                    break;

                // Add multiple players to the database to use them on privadita
                case "$addPlayers":
                    if (comando.length > 1) {
                        String[] playersNames = Arrays.copyOfRange(comando, 1, comando.length);
                        List<Player> newPlayers = new ArrayList<>();
                        for (String name : playersNames) {
                            Player newPlayer = new Player();
                            newPlayer.setName(name);
                            newPlayers.add(newPlayer);
                        }
                        dbManager.AddPlayers(newPlayers);
                    } else {
                        event.getChannel().sendMessage("El comando $addPlayers necesita al menos un nombre de jugador").queue();
                    }
                    break;

                case "$verElo":
                    if (comando.length == 2) {
                        dbManager.ShowPlayerElo(comando[1]);
                    } else {
                        dbManager.ShowAllElo();
                    }
                    break;

                case "$deletePlayer":
                    if (comando.length == 2) {
                        dbManager.DeletePlayer(comando[1]);
                    } else {
                        event.getChannel().sendMessage("El comando $deletePlayer necesita un nombre de jugador").queue();
                    }
                    break;
                default:
                    event.getChannel().sendMessage("Comando no encontrado").queue();


            }
        }
    }

    private void StartPrivadita(String[] comando, MessageReceivedEvent event) {
        String[] playersNames = Arrays.copyOfRange(comando, 1, comando.length);

        players = CheckPrivaditaCommand(playersNames, event);

        // if its repeated we don't want to continue
        if (players == null) {
            return;
        }

        List<Player> playersNotFound = dbManager.GetPlayersNotFound(players);

        if (!playersNotFound.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("```")
                    .append("Los siguientes jugadores no están en la base de datos: \n");
            for (Player player : playersNotFound) {
                messageBuilder.append(player.getName())
                        .append("\n");
            }
            messageBuilder.append("Usa el comando $addPlayer para añadirlos a la base de datos");
            messageBuilder.append("```");

            String formattedMessage = messageBuilder.toString();
            event.getChannel().sendMessage(formattedMessage).queue();
            players.clear();
            return;
        }

        activeGame.set(true);

        ObservableList<Player> blueTeam = new SimpleListProperty<>(FXCollections.observableArrayList());
        ObservableList<Player> redTeam = new SimpleListProperty<>(FXCollections.observableArrayList());

        // Shuffle the players so the teams are random
        Collections.shuffle(players);

        // Divide the players in two teams
        for (int i = 0; i < 5; i++) {
            blueTeam.add(players.get(i));
            redTeam.add(players.get(i + 5));
        }

        // Assign the roles to the players
        for (int i = 0; i < 5; i++) {
            blueTeam.get(i).setRole(roles.get(i));
            redTeam.get(i).setRole(roles.get(i));
        }

        // Create the message with the 2 teams
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("```")
                .append("\nBlue Team\n");
        for (Player player : blueTeam) {
            messageBuilder.append(player.getName())
                    .append(" -> ")
                    .append(player.getRole())
                    .append("\n");
        }

        messageBuilder.append("\nRed Team\n");
        for (Player player : redTeam) {
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
    private ListProperty<Player> CheckPrivaditaCommand(String[] playersNames, MessageReceivedEvent event) {
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

    public ListProperty<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(ListProperty<LogEntry> logs) {
        this.logs = logs;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public ListProperty<Player> getPlayers() {
        return players;
    }

    public void setPlayers(ListProperty<Player> players) {
        this.players = players;
    }

    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }
}
