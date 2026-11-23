package dad.api;

import dad.api.commands.RollaDie;
import dad.api.models.LogEntry;
import dad.api.commands.Privadita;
import dad.audio.PlayerManager;
import dad.audio.GuildMusicManager;
import dad.database.DBManager;
import dad.database.Player;
import dad.season.FirestoreSeasonStore;
import dad.season.SeasonArchive;
import dad.season.SeasonCalendar;
import dad.season.SeasonCsv;
import dad.season.SeasonManager;
import dad.season.SeasonScheduler;
import dad.season.SeasonTable;
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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import net.dv8tion.jda.api.utils.FileUpload;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


import java.io.InputStream;
import java.nio.file.Path;
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

    /** Comandos que leen o escriben Elo: antes de ejecutarlos se comprueba si toca cambiar de temporada. */
    private static final Set<String> SEASON_COMMANDS = Set.of(
            "$privadita", "$resultadoPrivadita", "$resultadoprivadita", "$verElo", "$verEscalera",
            "$csvTemporada", "$csvtemporada", "$verTemporada", "$vertemporada", "$historico", "$histórico",
            "$addPlayer", "$addPlayers", "$deletePlayer", "$adminResetElo", "$adminresetelo");

    /** Canal de Discord donde se anuncia el fin de temporada (sobrescribible con season.announceChannelId). */
    private static final String DEFAULT_SEASON_ANNOUNCE_CHANNEL = "1309584794744524910";

    // Sistema de temporadas (null si Firebase no se pudo inicializar)
    private SeasonManager seasonManager;
    private SeasonScheduler seasonScheduler;

    public Commands() {
        Utils.loadProperties();
        loadCommandStatus();
        initSeasons();
    }

    /**
     * Arranca el sistema de temporadas: la comprobación de fechas corre en segundo plano (no depende de que
     * nadie use un comando) y usa siempre la conexión JDA actual, así que sobrevive a reconexiones.
     */
    private void initSeasons() {
        if (dbManager.getDb() == null) {
            System.err.println("⚠️ [Temporadas] Firebase no está disponible: el sistema de temporadas queda desactivado.");
            return;
        }
        SeasonArchive archive = new SeasonArchive(SeasonArchive.defaultDir(Utils.properties.getProperty("season.archiveDir")));
        seasonManager = new SeasonManager(new FirestoreSeasonStore(dbManager.getDb()), archive, SeasonCalendar::today);
        seasonManager.setListener(this::onSeasonClosed);
        seasonScheduler = new SeasonScheduler(seasonManager, this::getJda);
        seasonScheduler.start();
    }

    // checks if the commands are active
    private void loadCommandStatus() {
        // fill with the commands
        boolean rollaDieActive = Boolean.parseBoolean(Utils.properties.getProperty("rollaDie", "true"));
        setIsActive(rollaDieActive);
    }

    /** En cuanto Discord está listo se comprueba la temporada, sin esperar al primer ciclo del planificador. */
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        if (seasonScheduler != null) {
            seasonScheduler.checkNow();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            processMessage(event);
        } catch (Exception e) {
            // Red de seguridad: ningún comando debe quedarse a medias sin responder
            e.printStackTrace();
            event.getChannel().sendMessage("❌ **Ha ocurrido un error inesperado al ejecutar el comando.** Inténtalo de nuevo.").queue();
        }
    }

    private void processMessage(MessageReceivedEvent event) {
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
            // Si ha terminado la temporada, se cierra ANTES de responder: nunca se muestran ni se
            // modifican datos de la temporada anterior
            if (SEASON_COMMANDS.contains(comando[0])) {
                ensureSeasonUpToDate(event);
            }

            switch (comando[0]) {

                // Start a privadita
                case "$privadita":
                    runSeasonLocked(event, () -> handlePrivadita(event, comando));
                    break;

                // Set the winner of the privadita
                case "$resultadoPrivadita":
                case "$resultadoprivadita":
                    handleResultadoPrivadita(event, comando);
                    break;

                // Send the CSV of a finished season
                case "$csvTemporada":
                case "$csvtemporada":
                    handleCsvTemporada(event, comando);
                    break;

                // Show the final table of a finished season / list all finished seasons
                case "$verTemporada":
                case "$vertemporada":
                    handleVerTemporada(event, comando);
                    break;

                case "$historico":
                case "$histórico":
                    handleHistorico(event);
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
                
                case "$verEscalera":
                    handleVerEscalera(event);
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

    // Start a privadita
    private void handlePrivadita(MessageReceivedEvent event, String[] comando) {
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
                // Solo se guarda si pasó la validación (nombres repetidos, etc.); si no, quedaría
                // una privadita rota que haría fallar $resultadoPrivadita
                if (nuevaPrivadita.isValid()) {
                    dbManager.setActivePrivadita(event, nuevaPrivadita);
                }
                
                // event.getChannel().sendMessage(nuevaPrivadita.toString()).queue();
            } else {
                event.getChannel().sendMessage("❌ **Uno o más jugadores no fueron encontrados en la base de datos de este servidor.**").queue();
            }
        } else {
            event.getChannel().sendMessage("❌ **El comando $privadita necesita 10 jugadores.**").queue();
        }
    }

    /**
     * $resultadoPrivadita <blue|red>
     * - El equipo se normaliza (espacios y mayúsculas/minúsculas) ANTES de tocar nada.
     * - Una entrada inválida no cancela la privadita: se puede volver a mandar el resultado correcto.
     * - El resultado se calcula, se guarda en Firebase y solo entonces se anuncia y se limpia la privadita.
     *   Si algo falla, se deshacen los cambios en memoria y la privadita sigue activa para reintentar.
     */
    private void handleResultadoPrivadita(MessageReceivedEvent event, String[] comando) {
        String arg = firstArgument(comando);
        if (arg == null) {
            event.getChannel().sendMessage("❌ **Uso:** `$resultadoPrivadita <blue|red>`").queue();
            return;
        }
        Optional<Privadita.Team> ganador = Privadita.Team.parse(arg);
        if (ganador.isEmpty()) {
            event.getChannel().sendMessage("❌ **Equipo no válido:** `" + sanitize(arg) + "`. Usa `$resultadoPrivadita blue` o "
                    + "`$resultadoPrivadita red` (da igual mayúsculas o minúsculas). La privadita sigue activa.").queue();
            return;
        }

        runSeasonLocked(event, () -> {
            if (!dbManager.hasActivePrivadita(event)) {
                event.getChannel().sendMessage("❌ **No hay ninguna privadita activa en este servidor.**").queue();
                return;
            }
            Privadita privadita = dbManager.getActivePrivadita(event);
            List<Privadita.StatsSnapshot> antes = privadita.snapshotStats();
            try {
                String resultado = privadita.aplicarResultado(ganador.get());

                if (!dbManager.updatePlayers(event, privadita.getPlayers())) {
                    Privadita.restoreStats(antes);
                    event.getChannel().sendMessage("❌ **No se pudo guardar el resultado en la base de datos.** "
                            + "No se ha registrado nada y la privadita sigue activa: vuelve a usar `$resultadoPrivadita <blue|red>`.").queue();
                    return;
                }

                dbManager.clearActivePrivadita(event);
                event.getChannel().sendMessage(resultado).queue();
            } catch (Exception e) {
                Privadita.restoreStats(antes);
                e.printStackTrace();
                event.getChannel().sendMessage("❌ **Error al registrar el resultado:** " + sanitize(describe(e)) + "\n"
                        + "No se ha registrado nada y la privadita sigue activa. Vuelve a intentarlo o usa `$dropPrivadita`.").queue();
            }
        });
    }

    // ==================== TEMPORADAS ====================

    /** $csvTemporada [n] → envía el CSV de la temporada n, o de la última terminada si no se indica. */
    private void handleCsvTemporada(MessageReceivedEvent event, String[] comando) {
        OptionalLong temporada = resolveFinishedSeason(event, comando, "$csvTemporada");
        if (temporada.isEmpty()) {
            return;
        }
        Optional<Path> csv;
        try {
            csv = seasonManager.findCsv(event.getGuild().getId(), temporada.getAsLong()); // caché local o Firestore
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("❌ **No se pudo recuperar el CSV:** " + sanitize(describe(e))).queue();
            return;
        }
        if (csv.isEmpty()) {
            sendSeasonNotFound(event, temporada.getAsLong());
            return;
        }
        File file = csv.get().toFile();
        event.getChannel().sendMessage("📄 **Clasificación final de la " + seasonName(temporada.getAsLong()) + "**")
                .addFiles(FileUpload.fromData(file, file.getName()))
                .queue(null, error -> event.getChannel()
                        .sendMessage("❌ **No se pudo enviar el archivo:** " + sanitize(describe(error))).queue());
    }

    /**
     * $verTemporada [n] → muestra la clasificación final de una temporada terminada con la misma tabla
     * que $verElo (se genera a partir del CSV guardado). Sin número: la última terminada.
     */
    private void handleVerTemporada(MessageReceivedEvent event, String[] comando) {
        OptionalLong temporada = resolveFinishedSeason(event, comando, "$verTemporada");
        if (temporada.isEmpty()) {
            return;
        }
        long n = temporada.getAsLong();
        Optional<String> csv;
        try {
            csv = seasonManager.readCsv(event.getGuild().getId(), n);
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("❌ **No se pudo recuperar la clasificación:** " + sanitize(describe(e))).queue();
            return;
        }
        if (csv.isEmpty()) {
            sendSeasonNotFound(event, n);
            return;
        }
        String title = "📅 " + capitalize(seasonName(n)) + " (" + seasonRange(n) + ") — finalizada\n\n"
                + "🏆 **CLASIFICACIÓN FINAL - " + event.getGuild().getName() + "**";
        for (String part : SeasonTable.render(title, SeasonCsv.parse(csv.get()))) {
            event.getChannel().sendMessage(part).queue();
        }
    }

    /** $historico → lista las temporadas terminadas con su campeón, de la más reciente a la más antigua. */
    private void handleHistorico(MessageReceivedEvent event) {
        if (seasonManager == null) {
            event.getChannel().sendMessage("❌ **El sistema de temporadas no está disponible** (no hay conexión con la base de datos).").queue();
            return;
        }
        String guildId = event.getGuild().getId();
        StringBuilder msg = new StringBuilder("📚 **HISTÓRICO DE TEMPORADAS - ").append(event.getGuild().getName()).append("**\n\n");
        try {
            List<Long> seasons = seasonManager.archivedSeasons(guildId);
            if (seasons.isEmpty()) {
                event.getChannel().sendMessage("ℹ️ **Todavía no ha terminado ninguna temporada en este servidor.**").queue();
                return;
            }
            for (long n : seasons) {
                List<SeasonCsv.Row> rows = SeasonCsv.parse(seasonManager.readCsv(guildId, n).orElse(""));
                String line = "• **" + capitalize(seasonName(n)) + "** (" + seasonRange(n) + ") — "
                        + (rows.isEmpty() ? "sin jugadores"
                            : "🏆 " + rows.get(0).name() + " (" + rows.get(0).elo() + ") · " + rows.size() + " jugadores")
                        + "\n";
                if (msg.length() + line.length() > SeasonTable.DISCORD_LIMIT - 150) {
                    msg.append("…\n");
                    break;
                }
                msg.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("❌ **No se pudo consultar el histórico de temporadas:** " + sanitize(describe(e))).queue();
            return;
        }
        msg.append("\nUsa `$verTemporada <número>` para ver la tabla o `$csvTemporada <número>` para descargar el CSV.");
        event.getChannel().sendMessage(msg.toString()).queue();
    }

    /**
     * Temporada terminada pedida en el comando (o la última terminada si no se indica).
     * Si no es válida responde al usuario y devuelve vacío.
     */
    private OptionalLong resolveFinishedSeason(MessageReceivedEvent event, String[] comando, String commandName) {
        if (seasonManager == null) {
            event.getChannel().sendMessage("❌ **El sistema de temporadas no está disponible** (no hay conexión con la base de datos).").queue();
            return OptionalLong.empty();
        }
        String arg = firstArgument(comando);
        if (arg == null) {
            OptionalLong last;
            try {
                last = seasonManager.latestArchivedSeason(event.getGuild().getId());
            } catch (Exception e) {
                e.printStackTrace();
                event.getChannel().sendMessage("❌ **No se pudo consultar el histórico de temporadas:** " + sanitize(describe(e))).queue();
                return OptionalLong.empty();
            }
            if (last.isEmpty()) {
                event.getChannel().sendMessage("ℹ️ **Todavía no ha terminado ninguna temporada en este servidor.**"
                        + (seasonManager.currentSeasonByDate() == SeasonCalendar.PRESEASON
                            ? " La Temporada 1 empieza el " + SeasonCalendar.human(SeasonCalendar.FIRST_SEASON_START) + "."
                            : "")).queue();
            }
            return last;
        }
        String num = arg.toLowerCase(Locale.ROOT).replaceFirst("^(temporada|t)", "");
        try {
            long n = Long.parseLong(num);
            if (n >= 0) {
                return OptionalLong.of(n);
            }
        } catch (NumberFormatException ignored) {
            // cae al mensaje de uso
        }
        event.getChannel().sendMessage("❌ **Uso:** `" + commandName + "` (última terminada) o `" + commandName
                + " <número>` — p. ej. `" + commandName + " 1`. La pretemporada es la `0`.").queue();
        return OptionalLong.empty();
    }

    private void sendSeasonNotFound(MessageReceivedEvent event, long temporada) {
        long actual = seasonManager.currentSeasonByDate();
        if (temporada > actual) {
            event.getChannel().sendMessage("⏳ **La " + seasonName(temporada) + " todavía no ha empezado.**").queue();
        } else if (temporada == actual) {
            event.getChannel().sendMessage("⏳ **La " + seasonName(temporada) + " todavía no ha terminado** (termina el "
                    + SeasonCalendar.human(SeasonCalendar.lastDay(temporada)) + ").").queue();
        } else {
            event.getChannel().sendMessage("❌ **No hay clasificación guardada de la " + seasonName(temporada) + " en este servidor.** "
                    + "Puede que la temporada terminara sin jugadores registrados.").queue();
        }
    }

    /** "23 nov 2026 - 22 feb 2027" (la pretemporada: "hasta el 22 nov 2026"). */
    private static String seasonRange(long n) {
        if (n == SeasonCalendar.PRESEASON) {
            return "hasta el " + SeasonCalendar.human(SeasonCalendar.lastDay(n));
        }
        return SeasonCalendar.human(SeasonCalendar.startOf(n)) + " - " + SeasonCalendar.human(SeasonCalendar.lastDay(n));
    }

    /** Se llama al cerrar una temporada (desde el hilo del planificador o desde un comando). */
    private void onSeasonClosed(String guildId, long closedSeason, long newSeason, Optional<Path> csv) {
        // Una privadita creada con los Elo de la temporada anterior no debe registrarse en la nueva
        boolean privaditaCancelada = dbManager.clearActivePrivadita(guildId);

        JDA current = jda;
        Guild guild = current != null ? current.getGuildById(guildId) : null;
        if (guild == null) {
            return;
        }
        TextChannel channel = seasonAnnounceChannel(guild);
        if (channel == null) {
            System.out.println("ℹ️ [Temporadas] " + guild.getName() + ": sin canal donde anunciar el cambio de temporada");
            return;
        }
        StringBuilder msg = new StringBuilder()
                .append("🏁 **").append(capitalize(seasonName(closedSeason))).append(" terminada.** ")
                .append("Empieza la **Temporada ").append(newSeason).append("** (")
                .append(SeasonCalendar.human(SeasonCalendar.startOf(newSeason))).append(" – ")
                .append(SeasonCalendar.human(SeasonCalendar.lastDay(newSeason))).append(").\n")
                .append("Elo, victorias y derrotas reiniciados (Elo ").append(FirestoreSeasonStore.DEFAULT_ELO).append(").");
        if (privaditaCancelada) {
            msg.append("\n⚠️ La privadita que estaba en curso se ha cancelado por el cambio de temporada.");
        }
        if (csv.isPresent()) {
            msg.append("\n📄 Clasificación final adjunta (también con `$csvTemporada ").append(closedSeason).append("`).");
            File file = csv.get().toFile();
            channel.sendMessage(msg.toString()).addFiles(FileUpload.fromData(file, file.getName())).queue();
        } else {
            channel.sendMessage(msg.toString()).queue();
        }
    }

    /**
     * Canal donde se anuncia el fin de temporada (con el CSV adjunto).
     * Por defecto el canal {@link #DEFAULT_SEASON_ANNOUNCE_CHANNEL}; se puede cambiar con la propiedad
     * {@code season.announceChannelId} de settings.properties (varios IDs separados por comas, uno por servidor).
     * Si el servidor no tiene ninguno de esos canales, o el bot no puede escribir en él, se usa su canal de sistema.
     */
    private TextChannel seasonAnnounceChannel(Guild guild) {
        String ids = Utils.properties.getProperty("season.announceChannelId", DEFAULT_SEASON_ANNOUNCE_CHANNEL);
        for (String id : ids.split(",")) {
            String trimmed = id.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                TextChannel configured = guild.getTextChannelById(trimmed);
                if (configured != null) {
                    if (configured.canTalk()) {
                        return configured;
                    }
                    System.out.println("⚠️ [Temporadas] " + guild.getName() + ": sin permiso para escribir en #" + configured.getName());
                }
            } catch (NumberFormatException e) {
                System.err.println("⚠️ [Temporadas] season.announceChannelId no válido: '" + trimmed + "'");
            }
        }
        TextChannel system = guild.getSystemChannel();
        return system != null && system.canTalk() ? system : null;
    }

    /**
     * Comprobación al usar comandos de Elo (además del planificador en segundo plano). Si el cambio de
     * temporada falla, se avisa en el chat en vez de mostrar datos de la temporada anterior sin decir nada.
     */
    private void ensureSeasonUpToDate(MessageReceivedEvent event) {
        if (seasonManager == null) {
            return;
        }
        try {
            seasonManager.ensureCurrent(event.getGuild().getId());
        } catch (Exception e) {
            // No bloquea el comando: el planificador lo reintentará
            e.printStackTrace();
            event.getChannel().sendMessage("⚠️ **El cambio de temporada está pendiente** (error: " + sanitize(describe(e))
                    + "). Se reintentará automáticamente; mientras tanto los datos pueden ser de la temporada anterior.").queue();
        }
    }

    private void runSeasonLocked(MessageReceivedEvent event, Runnable action) {
        if (seasonManager == null) {
            action.run();
        } else {
            seasonManager.runLocked(event.getGuild().getId(), action);
        }
    }

    private static String seasonName(long season) {
        return season == SeasonCalendar.PRESEASON ? "pretemporada" : "Temporada " + season;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Primer argumento no vacío (tolera dobles espacios: "$resultadoPrivadita   red"). */
    private static String firstArgument(String[] comando) {
        for (int i = 1; i < comando.length; i++) {
            String t = comando[i].trim();
            if (!t.isEmpty()) {
                return t.split("\\s+")[0];
            }
        }
        return null;
    }

    private static String describe(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    /** Evita que un texto del usuario rompa el formato o mencione a @everyone. */
    private static String sanitize(String text) {
        String t = text.replace("`", "").replace("@", "");
        return t.length() > 60 ? t.substring(0, 60) + "…" : t;
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

        // ==================== ELO LADDER (JENKS) ====================

    private void handleVerEscalera(MessageReceivedEvent event) {
        // Get all players for this server
        List<Player> allPlayers = dbManager.getAllPlayers(event);

        if (allPlayers == null || allPlayers.isEmpty()) {
            event.getChannel().sendMessage("❌ **No players found in the database for this server.**").queue();
            return;
        }

        // Sort players by Elo descending
        allPlayers.sort(Comparator.comparingInt(Player::getElo).reversed());

        // Decide number of classes for Jenks (simple heuristic)
        int numClasses = Math.min(5, Math.max(2, (int) Math.round(Math.sqrt(allPlayers.size()))));

        // Compute Jenks breaks
        double[] breaks = computeJenksBreaks(allPlayers, numClasses);

        // Group players by tier according to breaks
        Map<Integer, List<Player>> tiers = clusterPlayersByJenks(allPlayers, breaks);

        try {
            File imageFile = generateLadderImage(tiers, breaks, event.getGuild().getName());
            event.getChannel()
                    .sendFiles(FileUpload.fromData(imageFile, "escalera_elo.png"))
                    .queue();
        } catch (IOException e) {
            e.printStackTrace();
            event.getChannel().sendMessage("❌ **Error while generating the ladder image.**").queue();
        }
    }

    /**
     * Compute Jenks natural breaks for player Elo values.
     */
    private double[] computeJenksBreaks(List<Player> players, int numClasses) {
        int n = players.size();
        double[] data = new double[n];
        for (int i = 0; i < n; i++) {
            data[i] = players.get(i).getElo();
        }

        // Sort ascending for Jenks
        Arrays.sort(data);

        double[][] mat1 = new double[n + 1][numClasses + 1];
        double[][] mat2 = new double[n + 1][numClasses + 1];

        for (int i = 1; i <= numClasses; i++) {
            mat1[0][i] = 1.0;
            mat2[0][i] = 0.0;
            for (int j = 1; j <= n; j++) {
                mat2[j][i] = Double.MAX_VALUE;
            }
        }

        double sum, sumSq, w, variance = 0.0;
        for (int l = 1; l <= n; l++) {
            sum = 0.0;
            sumSq = 0.0;
            w = 0.0;

            for (int m = 1; m <= l; m++) {
                int i3 = l - m + 1;
                double val = data[i3 - 1];

                w += 1.0;
                sum += val;
                sumSq += val * val;

                variance = sumSq - (sum * sum) / w;
                int i4 = i3 - 1;

                if (i4 != 0) {
                    for (int j = 2; j <= numClasses; j++) {
                        if (mat2[l][j] >= variance + mat2[i4][j - 1]) {
                            mat1[l][j] = i3;
                            mat2[l][j] = variance + mat2[i4][j - 1];
                        }
                    }
                }
            }
            mat1[l][1] = 1.0;
            mat2[l][1] = variance;
        }

        double[] breaks = new double[numClasses];
        breaks[numClasses - 1] = data[n - 1];

        int k = n;
        for (int j = numClasses; j >= 2; j--) {
            int id = (int) mat1[k][j] - 2;
            breaks[j - 2] = data[id];
            k = (int) mat1[k][j] - 1;
        }

        return breaks;
    }

    /**
     * Assign players to tiers based on Jenks breaks.
     * Key: tier index (0 = lowest, numClasses-1 = highest).
     */
    private Map<Integer, List<Player>> clusterPlayersByJenks(List<Player> players, double[] breaks) {
        Map<Integer, List<Player>> tiers = new HashMap<>();
        int numClasses = breaks.length;

        for (int i = 0; i < numClasses; i++) {
            tiers.put(i, new ArrayList<>());
        }

        for (Player p : players) {
            int elo = p.getElo();
            int tierIndex = 0;
            for (int i = 0; i < numClasses; i++) {
                if (elo <= breaks[i]) {
                    tierIndex = i;
                    break;
                }
            }
            tiers.get(tierIndex).add(p);
        }

        return tiers;
    }

    private File generateLadderImage(Map<Integer, List<Player>> tiers, double[] breaks, String guildName) throws IOException {
        int margin = 60;
        int stepWidth = 180;
        int stepHeight = 90;
        int lineWidth = 6;
        int titleHeight = 50;
        int lineSpacing = 18;
        int extraTopOffset = 120;     // extra space between title and staircase
        int bottomLabelOffset = 60;  // space at the bottom for tier labels

        // tiers: 0 = lowest, last = highest
        List<Integer> tierIds = new ArrayList<>(tiers.keySet());
        Collections.sort(tierIds);
        int numTiers = tierIds.size();

        int width = margin * 2 + stepWidth * numTiers + 250;
        int height = margin * 2 + titleHeight + extraTopOffset
                + stepHeight * (numTiers + 1) + bottomLabelOffset;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // === Background with gradient ===
        GradientPaint gp = new GradientPaint(
                0, 0, new Color(18, 18, 24),
                0, height, new Color(5, 5, 8)
        );
        g.setPaint(gp);
        g.fillRect(0, 0, width, height);

        // === Title with slight shadow ===
        String title = "Elo Staircase - " + guildName;
        Font titleFont = new Font("SansSerif", Font.BOLD, 30);
        g.setFont(titleFont);
        FontMetrics fmTitle = g.getFontMetrics();
        int titleX = margin;
        int titleY = margin + fmTitle.getAscent();

        g.setColor(new Color(0, 0, 0, 150)); // shadow
        g.drawString(title, titleX + 3, titleY + 3);
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, titleY);

        // Start of staircase (bottom-left)
        int baseY = margin + titleHeight + extraTopOffset + stepHeight * (numTiers - 1);
        int baseX = margin;

        g.setStroke(new BasicStroke(lineWidth));

        // For bottom tier labels
        int bottomLabelY = baseY + 40;
        int[] tierLabelCenters = new int[numTiers]; // index = humanTier-1 → x center

        // === Draw steps from lowest (left/bottom) to highest (right/top) ===
        for (int i = 0; i < numTiers; i++) {
            int tierId = tierIds.get(i);
            List<Player> playersInTier = tiers.get(tierId);
            if (playersInTier == null || playersInTier.isEmpty()) {
                continue;
            }

            // Ensure players are sorted by Elo descending inside each tier
            playersInTier.sort(Comparator.comparingInt(Player::getElo).reversed());

            int xStart = baseX + i * stepWidth;
            int y = baseY - i * stepHeight;
            int xEnd = xStart + stepWidth;

            // Slight color variation per step
            float ratio = (float) i / Math.max(1, numTiers - 1);
            Color stepColor = new Color(
                    (int) (220 + 20 * ratio),
                    (int) (200 - 60 * ratio),
                    255
            );
            g.setColor(stepColor);

            // Horizontal step
            g.drawLine(xStart, y, xEnd, y);

            // Vertical rise to next step (except last)
            if (i < numTiers - 1) {
                g.drawLine(xEnd, y, xEnd, y - stepHeight);
            }

            // === Player names ABOVE the step, TOP = highest Elo ===
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            int textX = xStart + 10;
            // start higher so the first player (best Elo) quede arriba del todo
            int textY = y - 10 - (playersInTier.size() - 1) * lineSpacing;

            for (Player p : playersInTier) {
                String label = p.getName() + " (" + p.getElo() + ")";

                // Shadow
                g.setColor(new Color(0, 0, 0, 180));
                g.drawString(label, textX + 1, textY + 1);

                // Main text
                g.setColor(Color.WHITE);
                g.drawString(label, textX, textY);

                textY += lineSpacing; // now we go DOWNWARDS
            }

            // Save center X for bottom tier label
            int humanTierNumber = numTiers - i; // top step => Tier 1
            int centerX = xStart + stepWidth / 2;
            tierLabelCenters[humanTierNumber - 1] = centerX;
        }

        // === Draw tier labels aligned at bottom ===
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        for (int humanTier = 1; humanTier <= numTiers; humanTier++) {
            int centerX = tierLabelCenters[humanTier - 1];
            if (centerX == 0) continue;

            String tierText = "Tier " + humanTier;
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(tierText);

            // Shadow
            g.setColor(new Color(0, 0, 0, 180));
            g.drawString(tierText, centerX - textWidth / 2 + 2, bottomLabelY + 2);

            // Text
            g.setColor(new Color(230, 230, 230));
            g.drawString(tierText, centerX - textWidth / 2, bottomLabelY);
        }

        g.dispose();

        File out = File.createTempFile("elo_staircase_", ".png");
        ImageIO.write(image, "png", out);
        return out;
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