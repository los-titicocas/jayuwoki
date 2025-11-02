package dad.api;

import dad.api.commands.RollaDie;
import dad.api.models.LogEntry;
import dad.api.commands.Privadita;
import dad.audio.PlayerManager;
import dad.audio.GuildMusicManager;
import dad.database.DBManager;
import dad.database.Player;
import dad.utils.Utils;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Commands extends ListenerAdapter {

    // Command objects
    // ❌ ELIMINAR: Ya no usamos esta lista local, ahora se gestiona en DBManager por servidor
    // private ListProperty<Privadita> privaditas = new SimpleListProperty<>(FXCollections.observableArrayList());
    
    private ListProperty<LogEntry> logs = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final DBManager dbManager = new DBManager();
    protected JDA jda;

    // define the state of the command
    private final BooleanProperty isActive = new SimpleBooleanProperty(true);

    public Commands() {
        Utils.loadProperties();
        loadCommandStatus();
    }

    // checks if the commands are active
    private void loadCommandStatus() {
        // fill with the commands
        boolean rollaDieActive = Boolean.parseBoolean(Utils.properties.getProperty("rollaDie", "true"));
        setIsActive(rollaDieActive);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (message.startsWith("$")) {
            dbManager.setEvent(event);
            // ❌ ELIMINAR esta línea (ya no existe setCurrentServer):
            // dbManager.setCurrentServer(event.getGuild().getName());

            String[] comando = message.split(" ", 11);

            // Introduce the command in the log
            LogEntry logEntry = new LogEntry(event.getAuthor().getName(), message, event.getMessage().getTimeCreated().toLocalDateTime());
            logs.add(logEntry);

            // Switch with all the possible commands
            switch (comando[0]) {

                // Start a privadita
                case "$privadita":
                    // ✅ ACTUALIZAR: Verificar privadita activa usando DBManager
                    if (dbManager.hasActivePrivadita(event)) {
                        event.getChannel().sendMessage("❌ **Ya hay una privadita activa en este servidor.**\n" +
                                "Usa `$dropPrivadita` para cancelarla primero.").queue();
                    } else if (comando.length == 11) {
                        // ✅ Normalizar nombres de jugadores
                        String[] normalizedNames = new String[10];
                        for (int i = 0; i < 10; i++) {
                            normalizedNames[i] = comando[i + 1].toLowerCase().trim();
                        }
                        
                        // Obtener jugadores desde la base de datos usando DBManager
                        List<Player> players = dbManager.GetPlayers(normalizedNames, event);

                        if (players.size() == 10) {
                            // ✅ ACTUALIZAR: Crear y guardar privadita en DBManager
                            Privadita nuevaPrivadita = new Privadita(players, event);
                            dbManager.setActivePrivadita(event, nuevaPrivadita);
                            
                            // event.getChannel().sendMessage(nuevaPrivadita.toString()).queue();
                        } else {
                            event.getChannel().sendMessage("❌ **Uno o más jugadores no fueron encontrados en la base de datos de este servidor.**").queue();
                        }
                    } else {
                        event.getChannel().sendMessage("❌ **El comando $privadita necesita 10 jugadores.**").queue();
                    }
                    break;

                // Set the winner of the privadita
                case "$resultadoPrivadita":
                    if (comando.length < 2) {
                        event.getChannel().sendMessage("❌ **Uso:** `$resultadoPrivadita <equipo1|equipo2>`").queue();
                        break;
                    }
                    
                    // ✅ ACTUALIZAR: Obtener privadita desde DBManager
                    if (!dbManager.hasActivePrivadita(event)) {
                        event.getChannel().sendMessage("❌ **No hay ninguna privadita activa en este servidor.**").queue();
                        break;
                    }
                    
                    Privadita privaditaResultado = dbManager.getActivePrivadita(event);
                    privaditaResultado.ResultadoPrivadita(comando[1], event);
                    
                    // Actualizar jugadores en Firebase
                    dbManager.updatePlayers(event, privaditaResultado.getPlayers());
                    
                    // Limpiar privadita
                    dbManager.clearActivePrivadita(event);
                    break;

                // Remove the current privadita
                case "$dropPrivadita":
                    // ✅ ACTUALIZAR: Eliminar privadita usando DBManager
                    if (dbManager.hasActivePrivadita(event)) {
                        dbManager.clearActivePrivadita(event);
                        event.getChannel().sendMessage("✅ **Privadita cancelada.**").queue();
                    } else {
                        event.getChannel().sendMessage("❌ **No hay ninguna privadita activa en este servidor.**").queue();
                    }
                    break;

                // Add only one player to the database to use it on privadita
                case "$addPlayer":
                    if (comando.length == 2) {
                        Player newPlayer = new Player();
                        newPlayer.setName(comando[1].toLowerCase().trim()); // ✅ Normalizar nombre
                        newPlayer.setElo(1000);
                        newPlayer.setWins(0);
                        newPlayer.setLosses(0);
                        dbManager.AddPlayer(newPlayer);
                    } else {
                        event.getChannel().sendMessage("❌ **El comando $addPlayer necesita un nombre de jugador.**").queue();
                    }
                    break;

                // Add multiple players to the database to use them on privadita
                case "$addPlayers":
                    if (comando.length > 1) {
                        String[] playersNames = Arrays.copyOfRange(comando, 1, comando.length);
                        List<Player> newPlayers = new ArrayList<>();
                        for (String name : playersNames) {
                            Player newPlayer = new Player();
                            newPlayer.setName(name.toLowerCase().trim()); // ✅ Normalizar nombre
                            newPlayer.setElo(1000);
                            newPlayer.setWins(0);
                            newPlayer.setLosses(0);
                            newPlayers.add(newPlayer);
                        }
                        dbManager.AddPlayers(newPlayers);
                    } else {
                        event.getChannel().sendMessage("❌ **El comando $addPlayers necesita al menos un nombre de jugador.**").queue();
                    }
                    break;

                case "$deletePlayer":
                    if (comando.length == 2) {
                        dbManager.DeletePlayer(comando[1].toLowerCase().trim()); // ✅ Normalizar nombre
                    } else {
                        event.getChannel().sendMessage("❌ **El comando $deletePlayer necesita un nombre de jugador.**").queue();
                    }
                    break;

                case "$verElo":
                    if (comando.length == 2) {
                        dbManager.ShowPlayerElo(comando[1].toLowerCase().trim()); // ✅ Normalizar nombre
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
                    // Verificar que haya argumentos (comando[1] en adelante)
                    if (comando.length < 2) {
                        event.getChannel().sendMessage("❌ **Uso:** `$play <URL o búsqueda>`\n" +
                                "**Ejemplos:**\n" +
                                "`$play https://www.youtube.com/watch?v=dQw4w9WgXcQ`\n" +
                                "`$play Never Gonna Give You Up`").queue();
                        break;
                    }

                    // Verificar que el usuario esté en un canal de voz
                    Member playMember = event.getMember();
                    if (playMember == null) {
                        event.getChannel().sendMessage("❌ **No se pudo verificar tu estado de voz.**").queue();
                        break;
                    }

                    GuildVoiceState playVoiceState = playMember.getVoiceState();
                    if (playVoiceState == null || !playVoiceState.inAudioChannel()) {
                        event.getChannel().sendMessage("❌ **Debes estar en un canal de voz para reproducir música!**").queue();
                        break;
                    }

                    // Unir al bot al canal de voz si no está conectado
                    AudioManager playAudioManager = event.getGuild().getAudioManager();
                    VoiceChannel playVoiceChannel = (VoiceChannel) playVoiceState.getChannel();

                    if (!playAudioManager.isConnected()) {
                        playAudioManager.openAudioConnection(playVoiceChannel);
                        event.getChannel().sendMessage("✅ **Conectado a** `" + playVoiceChannel.getName() + "`").queue();
                    }

                    // Construir URL o búsqueda (tomar desde comando[1] en adelante, NO desde comando[0])
                    String[] playArgs = Arrays.copyOfRange(comando, 1, comando.length);
                    String playInput = String.join(" ", playArgs);

                    // Si no es una URL, hacer búsqueda en YouTube
                    if (!playInput.startsWith("http://") && !playInput.startsWith("https://")) {
                        playInput = "ytsearch:" + playInput;
                    }

                    // Cargar y reproducir
                    PlayerManager.getInstance().loadAndPlay(event.getChannel().asTextChannel(), playInput);
                    break;

                case "$pause":
                    GuildMusicManager pauseMusicManager = PlayerManager.getInstance()
                            .getMusicManager(event.getGuild());

                    if (pauseMusicManager.getPlayer().getPlayingTrack() == null) {
                        event.getChannel().sendMessage("❌ **No hay nada reproduciéndose!**").queue();
                        break;
                    }

                    pauseMusicManager.getPlayer().setPaused(true);
                    event.getChannel().sendMessage("⏸️ **Pausado**").queue();
                    break;

                case "$resume":
                    GuildMusicManager resumeMusicManager = PlayerManager.getInstance()
                            .getMusicManager(event.getGuild());

                    if (resumeMusicManager.getPlayer().getPlayingTrack() == null) {
                        event.getChannel().sendMessage("❌ **No hay nada pausado!**").queue();
                        break;
                    }

                    resumeMusicManager.getPlayer().setPaused(false);
                    event.getChannel().sendMessage("▶️ **Reanudado**").queue();
                    break;

                case "$skip":
                    GuildMusicManager skipMusicManager = PlayerManager.getInstance()
                            .getMusicManager(event.getGuild());

                    if (skipMusicManager.getPlayer().getPlayingTrack() == null) {
                        event.getChannel().sendMessage("❌ **No hay nada reproduciéndose!**").queue();
                        break;
                    }

                    skipMusicManager.getScheduler().nextTrack();
                    event.getChannel().sendMessage("⏭️ **Canción saltada**").queue();
                    break;

                case "$stop":
                    GuildMusicManager stopMusicManager = PlayerManager.getInstance()
                            .getMusicManager(event.getGuild());

                    stopMusicManager.getScheduler().getQueue().clear();
                    stopMusicManager.getPlayer().stopTrack();
                    event.getGuild().getAudioManager().closeAudioConnection();
                    event.getChannel().sendMessage("⏹️ **Reproducción detenida y cola limpiada**").queue();
                    break;

                case "$queue":
                case "$cola":
                    GuildMusicManager queueMusicManager = PlayerManager.getInstance()
                            .getMusicManager(event.getGuild());

                    BlockingQueue<AudioTrack> queue = queueMusicManager.getScheduler().getQueue();

                    if (queueMusicManager.getPlayer().getPlayingTrack() == null && queue.isEmpty()) {
                        event.getChannel().sendMessage("❌ **La cola está vacía!**").queue();
                        break;
                    }

                    StringBuilder queueMessage = new StringBuilder("🎵 **Cola de reproducción:**\n\n");

                    // Canción actual
                    AudioTrack currentTrack = queueMusicManager.getPlayer().getPlayingTrack();
                    if (currentTrack != null) {
                        queueMessage.append("▶️ **Reproduciendo ahora:**\n")
                                   .append("`").append(currentTrack.getInfo().title).append("`")
                                   .append(" por `").append(currentTrack.getInfo().author).append("`\n\n");
                    }

                    // Próximas canciones
                    if (!queue.isEmpty()) {
                        queueMessage.append("**Próximas canciones:**\n");
                        List<AudioTrack> trackList = new ArrayList<>(queue);
                        int count = 1;
                        for (AudioTrack track : trackList) {
                            if (count > 10) {
                                queueMessage.append("\n*... y ").append(trackList.size() - 10)
                                           .append(" canciones más*");
                                break;
                            }
                            queueMessage.append(count++).append(". `")
                                       .append(track.getInfo().title).append("`\n");
                        }
                    }

                    event.getChannel().sendMessage(queueMessage.toString()).queue();
                    break;

                case "$nowplaying":
                case "$np":
                    GuildMusicManager npMusicManager = PlayerManager.getInstance()
                            .getMusicManager(event.getGuild());

                    AudioTrack npTrack = npMusicManager.getPlayer().getPlayingTrack();

                    if (npTrack == null) {
                        event.getChannel().sendMessage("❌ **No hay nada reproduciéndose!**").queue();
                        break;
                    }

                    long position = npTrack.getPosition() / 1000; // Convertir a segundos
                    long duration = npTrack.getDuration() / 1000;

                    String npMessage = String.format(
                        "🎵 **Reproduciendo ahora:**\n" +
                        "`%s`\n" +
                        "**Autor:** `%s`\n" +
                        "**Progreso:** `%d:%02d / %d:%02d`",
                        npTrack.getInfo().title,
                        npTrack.getInfo().author,
                        position / 60, position % 60,
                        duration / 60, duration % 60
                    );

                    event.getChannel().sendMessage(npMessage).queue();
                    break;

                case "$rolladie":
                    if (!checkCommandActive(event, "$rolladie")) {
                        break;
                    }
                    if (comando.length == 2) {
                        if (Integer.parseInt(comando[1]) > 20 || Integer.parseInt(comando[1]) < 2) {
                            event.getChannel().sendMessage("❌ **El dado debe tener entre 2 y 20 caras.**").queue();
                            break;
                        } else {
                            RollaDie rollaDie = new RollaDie(Integer.parseInt(comando[1]));
                            event.getChannel().sendMessage(rollaDie.toString()).queue();
                        }
                    } else {
                        event.getChannel().sendMessage("❌ **El comando $rolladie necesita argumentos.**").queue();
                    }
                    break;

                case "$help":
                    sendHelpMessage(event);
                    break;

                // Admin commands
                case "$adminResetElo":
                case "$adminresetelo":
                    if (comando.length < 2) {
                        event.getChannel().sendMessage("❌ **Uso:** `$adminResetElo <nombre>`\n" +
                                "Este comando resetea manualmente el Elo de un jugador a 1000.").queue();
                        return;
                    }
                    dbManager.AdminResetPlayerElo(comando[1].toLowerCase().trim()); // ✅ Normalizar nombre
                    break;

                default:
                    event.getChannel().sendMessage("❌ **Comando no encontrado.** Usa `$help` para ver los comandos disponibles.").queue();
            }
        }
    }

    public boolean checkCommandActive(MessageReceivedEvent event, String commandName) {
        if (!isActive()) {
            event.getChannel().sendMessage("❌ **El comando " + commandName + " está actualmente deshabilitado.**").queue();
            return false;
        }
        return true;
    }

    // Function to join the voice channel
    private void joinVoiceChannel(MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected()) {
            event.getChannel().sendMessage("✅ **Ya estoy conectado a un canal de voz.**").queue();
            return;
        }

        Member member = event.getMember();
        if (member != null && member.getVoiceState() != null && member.getVoiceState().getChannel() != null) {
            audioManager.openAudioConnection(member.getVoiceState().getChannel());
            event.getChannel().sendMessage("✅ **Conectado al canal de voz.**").queue();
        } else {
            event.getChannel().sendMessage("❌ **No estás en un canal de voz.**").queue();
        }
    }

    // Function to leave the voice channel
    private void leaveVoiceChannel(MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();

        if (audioManager.isConnected()) {
            audioManager.closeAudioConnection();
            event.getChannel().sendMessage("✅ **Desconectado del canal de voz.**").queue();
        } else {
            event.getChannel().sendMessage("❌ **No estoy conectado a ningún canal de voz.**").queue();
        }
    }

    private void sendHelpMessage(MessageReceivedEvent event) {
        try {
            // Leer el archivo Markdown que contiene los comandos
            InputStream inputStream = getClass().getResourceAsStream("/commands.md");
            if (inputStream == null) {
                event.getChannel().sendMessage("❌ **No se pudo encontrar el archivo de comandos.**").queue();
                return;
            }
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            String helpMessage = scanner.hasNext() ? scanner.next() : "No hay comandos disponibles.";
            scanner.close();

            // Enviar el mensaje de ayuda al DM
            event.getAuthor().openPrivateChannel().queue((channel) -> {
                channel.sendMessage(helpMessage).queue(
                    success -> event.getChannel().sendMessage("✅ **Te he enviado la lista de comandos por mensaje privado.**").queue(),
                    error -> event.getChannel().sendMessage("❌ **No pude enviarte un mensaje privado. Asegúrate de tener los DMs abiertos.**").queue()
                );
            });
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("❌ **Ocurrió un error al leer el archivo de comandos.**").queue();
        }
    }

    // change the state of the command
    public void setIsActive(boolean isActive) {
        this.isActive.set(isActive);
    }

    public void disableCommand() {
        setIsActive(false);
        Utils.properties.setProperty("rollaDie", "false");
        Utils.saveProperties();
    }

    public boolean isActive() {
        return isActive.get();
    }

    public BooleanProperty isActiveProperty() {
        return isActive;
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