package dad.api;

import dad.api.commands.RollaDie;
import dad.api.models.LogEntry;
import dad.api.models.Privadita;
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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.*;


public class Commands extends ListenerAdapter {

    private ListProperty<Privadita> privaditas = new SimpleListProperty<>(FXCollections.observableArrayList());

    private ListProperty<LogEntry> logs = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final DBManager dbManager = new DBManager();
    protected JDA jda;

    public Commands() {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
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
                    boolean isPrivaditaActive = privaditas.stream()
                            .anyMatch(privadita -> privadita.getServer().equals(event.getGuild().getName()));

                    if (isPrivaditaActive) {
                        event.getChannel().sendMessage("Ya hay una privadita en juego en este servidor. ACÁBALA (o usa $dropPrivadita)").queue();
                    } else if (comando.length == 11) {
                        String[] playersNames = Arrays.copyOfRange(comando, 1, comando.length);
                        ListProperty<Player> players = CheckPrivaditaCommand(playersNames, event);
                        if (players != null) {
                            Privadita nuevaPrivadita = new Privadita(players, event);
                            privaditas.add(nuevaPrivadita);
                        }
                    } else {
                        event.getChannel().sendMessage("El comando $privadita necesita 10 jugadores").queue();
                    }
                    break;

                // Remove the current privadita
                case "$dropPrivadita":
                    Privadita privaditaToDrop = privaditas.stream()
                            .filter(privadita -> privadita.getServer().equals(event.getGuild().getName()))
                            .findFirst()
                            .orElse(null);

                    if (privaditaToDrop != null) {
                        privaditas.remove(privaditaToDrop);
                        event.getChannel().sendMessage("Se ha cancelado la privadita").queue();
                    } else {
                        event.getChannel().sendMessage("No hay ninguna privadita activa en este servidor").queue();
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
                case "$join":
                    joinVoiceChannel(event);
                    break;

                case "$leave":
                    leaveVoiceChannel(event);
                    break;

                case "$play":
                    if (comando.length < 2) {
                        event.getChannel().sendMessage("Uso: $play <nombre del archivo>").queue();
                    } else {
                        String fileName = comando[1];
                        playAudio(event, fileName);
                    }
                    break;

                case "$rolladie":
                    if (comando.length == 2) {
                        if (Integer.parseInt(comando[1]) > 20 || Integer.parseInt(comando[1]) < 2) {
                            event.getChannel().sendMessage("The die must have between 2 and 20 sides").queue();
                            break;
                        } else {
                            RollaDie rollaDie = new RollaDie(Integer.parseInt(comando[1]));
                            event.getChannel().sendMessage(rollaDie.toString()).queue();
                        }
                    } else {
                        event.getChannel().sendMessage("The command $rolladie must have arguments").queue();
                    }
                    break;

                default:
                    event.getChannel().sendMessage("Comando no encontrado").queue();


            }
        }
    }

    // Function to join the voice channel

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


    // Function to leave the voice channel
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

    private void playAudio(MessageReceivedEvent event, String fileName) {
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

        String filePath = getClass().getResource("/audio/" + fileName).getPath();
        AudioHandler audioHandler = new AudioHandler(guild);
        audioHandler.loadAndPlay(filePath);

        event.getChannel().sendMessage("Reproduciendo audio: " + fileName).queue();
    }



    // FUnction to check the command and fill the player list
    private ListProperty<Player> CheckPrivaditaCommand(String[] playersNames, MessageReceivedEvent event) {
        Set<String> uniqueNames = new HashSet<>();
        ListProperty<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());

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

    public JDA getJda() {
        return jda;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }
}
