package dad.api;

import dad.api.commands.RollaDie;
import dad.api.models.LogEntry;
import dad.api.commands.Privadita;
import dad.api.music.GuildMusicManager;
import dad.api.music.PlayerManager;
import dad.database.Player;
import dad.database.DBManager;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.InputStream;
import java.util.*;

public class Commands extends ListenerAdapter {

    // Command objects
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

            String[] comando = message.split(" ", 11);

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
                        // Obtener jugadores desde la base de datos usando DBManager
                        List<Player> players = dbManager.GetPlayers(Arrays.copyOfRange(comando, 1, comando.length), event);

                        if (players.size() == 10) { // Asegurarse de que encontró los 10 jugadores
                            Privadita nuevaPrivadita = new Privadita(players, event);
                            privaditas.add(nuevaPrivadita);
                        } else {
                            event.getChannel().sendMessage("Uno o más jugadores no fueron encontrados en la base de datos.").queue();
                        }
                    } else {
                        event.getChannel().sendMessage("El comando $privadita necesita 10 jugadores").queue();
                    }
                    break;

                // Set the winner of the privadita
                case "$resultadoPrivadita":
                    Privadita privaditaResultado = privaditas.stream()
                            .filter(privadita -> privadita.getServer().equals(event.getGuild().getName()))
                            .findFirst()
                            .orElse(null);
                    if (privaditaResultado != null) {
                        privaditaResultado.ResultadoPrivadita(comando[1], event);
                        privaditas.remove(privaditaResultado);
                        dbManager.updatePlayers(privaditaResultado.getServer() ,privaditaResultado.getPlayers());
                    } else {
                        event.getChannel().sendMessage("No hay ninguna privadita activa en este servidor").queue();
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
                    newPlayer.setElo(1000);
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

                case "$deletePlayer":
                    if (comando.length == 2) {
                        dbManager.DeletePlayer(comando[1]);
                    } else {
                        event.getChannel().sendMessage("El comando $deletePlayer necesita un nombre de jugador").queue();
                    }
                    break;

                case "$verElo":
                    if (comando.length == 2) {
                        dbManager.ShowPlayerElo(comando[1]);
                    } else {
                        dbManager.ShowAllElo();
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
                        event.getChannel().sendMessage("Uso: $play <URL>").queue();
                    } else {
                        play(event, comando[1]);
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

                case "$help":
                    sendHelpMessage(event);
                    break;

                default:
                    event.getChannel().sendMessage("Comando no encontrado").queue();
            }
        }
    }

    // Function to join the voice channel
    private void joinVoiceChannel(MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected()) {
            event.getChannel().sendMessage("Ya estoy conectado a un canal de voz.").queue();
            return;
        }

        Member member = event.getMember();
        if (member != null && member.getVoiceState().getChannel() != null) {
            audioManager.openAudioConnection(member.getVoiceState().getChannel());
            event.getChannel().sendMessage("Conectado al canal de voz.").queue();
        } else {
            event.getChannel().sendMessage("No estás en un canal de voz.").queue();
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

    private void play(MessageReceivedEvent event, String trackUrl) {
        Guild guild = event.getGuild();
        PlayerManager playerManager = PlayerManager.get();
        playerManager.play(guild, trackUrl);
        event.getChannel().sendMessage("Reproduciendo: " + trackUrl).queue();
    }

    private void sendHelpMessage(MessageReceivedEvent event) {
        try {
            // Leer el archivo Markdown que contiene los comandos
            InputStream inputStream = getClass().getResourceAsStream("/commands.md");
            if (inputStream == null) {
                event.getChannel().sendMessage("No se pudo encontrar el archivo de comandos.").queue();
                return;
            }
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String helpMessage = scanner.hasNext() ? scanner.next() : "No hay comandos disponibles.";
            scanner.close();

            // Enviar el mensaje de ayuda
            event.getAuthor().openPrivateChannel().queue((channel) -> {
                channel.sendMessage(helpMessage).queue();
            });
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("Ocurrió un error al leer el archivo de comandos.").queue();
        }
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