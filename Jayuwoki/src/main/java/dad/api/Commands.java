package dad.api;

import dad.api.models.LogEntry;
import dad.api.models.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.*;

public class Commands extends ListenerAdapter {

    private final ArrayList<String> roles = new ArrayList<>(List.of("Top", "Jungla", "Mid", "ADC", "Support"));
    private ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ListProperty<LogEntry> logs = new SimpleListProperty<>(FXCollections.observableArrayList());
    private BooleanProperty activeGame = new SimpleBooleanProperty();

    public Commands() {
        activeGame.set(false);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (message.startsWith("$")) {
            String[] comando = message.split(" ");

            // Introduce the command in the log
            LogEntry logEntry = new LogEntry(event.getAuthor().getName(), message, event.getMessage().getTimeCreated().toLocalDateTime());
            logs.add(logEntry);

            // Switch with all the possible commands
            switch (comando[0]) {
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
                case "$dropPrivadita":
                    if (activeGame.get()) {
                        activeGame.set(false);
                        players.clear();
                        event.getChannel().sendMessage("Se ha cancelado la privadita").queue();
                    } else {
                        event.getChannel().sendMessage("No hay ninguna privadita activa").queue();
                    }
                    break;
                case "$join":
                    joinVoiceChannel(event);
                    break;
                case "$leave":
                    leaveVoiceChannel(event);
                    break;
                case "$play":
                    if (comando.length < 2) {
                        event.getChannel().sendMessage("Uso: $play <ruta del archivo>").queue();
                    } else {
                        String filePath = comando[1];
                        playAudio(event, filePath);
                    }
                    break;

                default:
                    event.getChannel().sendMessage("Comando no encontrado").queue();
            }
        }
    }

    private void playAudio(MessageReceivedEvent event, String filePath) {
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            event.getChannel().sendMessage("Debes estar en un canal de voz para usar este comando.").queue();
            return;
        }

        AudioChannel voiceChannel = member.getVoiceState().getChannel();
        AudioManager audioManager = guild.getAudioManager();

        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
        }

        dad.api.audio.AudioHandler audioHandler = new dad.api.audio.AudioHandler(guild);
        audioHandler.loadAndPlay(filePath);

        event.getChannel().sendMessage("Reproduciendo audio: " + filePath).queue();
    }


    private void StartPrivadita(String[] comando, MessageReceivedEvent event) {
        Set<String> uniqueNames = new HashSet<>();
        String[] playersNames = Arrays.copyOfRange(comando, 1, comando.length);
        boolean repeated = false;

        for (String name : playersNames) {
            if (!uniqueNames.add(name)) { // Si el nombre ya existe en el conjunto
                event.getChannel().sendMessage(name + " está repetido, prueba otra vez.").queue();
                repeated = true;
                break;
            }

            if (name.startsWith("$")) {
                event.getChannel().sendMessage("A donde vas listillo.").queue();
                repeated = true;
                break;
            }
            Player player = new Player();
            player.setName(name);
            players.add(player);
        }

        // if its repeated we don't want to continue
        if (repeated) {
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
            messageBuilder.append(player.getPlayerName())
                    .append(" -> ")
                    .append(player.getRole())
                    .append("\n");
        }

        messageBuilder.append("\nRed Team\n");
        for (Player player : redTeam) {
            messageBuilder.append(player.getPlayerName())
                    .append(" -> ")
                    .append(player.getRole())
                    .append("\n");
        }

        messageBuilder.append("```");

        String formattedMessage = messageBuilder.toString();
        event.getChannel().sendMessage(formattedMessage).queue();
    }

    private void joinVoiceChannel(MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        Member selfMember = guild.getSelfMember();
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected()) {
            event.getChannel().sendMessage("Ya estoy conectado a un canal de voz.").queue();
            return;
        }

        Member member = event.getMember();
        if (member != null) {
            AudioChannel voiceChannel = member.getVoiceState().getChannel();
            if (voiceChannel != null) {
                audioManager.openAudioConnection(voiceChannel);
                event.getChannel().sendMessage("Conectado al canal de voz: " + voiceChannel.getName()).queue();
            } else {
                event.getChannel().sendMessage("No estás en un canal de voz.").queue();
            }
        }
    }

    private void leaveVoiceChannel(MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
            event.getChannel().sendMessage("Desconectado del canal de voz.").queue();
        } else {
            event.getChannel().sendMessage("No estoy conectado a ningún canal de voz.").queue();
        }
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
}