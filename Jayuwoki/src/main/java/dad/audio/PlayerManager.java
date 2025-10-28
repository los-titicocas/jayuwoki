package dad.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona todos los reproductores de música para todos los servidores.
 */
public class PlayerManager {
    
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    private PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        
        // ✨ NUEVO: Registrar YouTube con soporte moderno
        YoutubeAudioSourceManager ytSourceManager = new YoutubeAudioSourceManager(
            true, // allowSearch - permite búsquedas
            new Music(),
            new Web(),
            new AndroidTestsuite(),
            new TvHtml5Embedded()
        );
        
        this.audioPlayerManager.registerSourceManager(ytSourceManager);
        
        // Registrar otras fuentes (SoundCloud, Bandcamp, etc.)
        AudioSourceManagers.registerRemoteSources(
            this.audioPlayerManager,
            com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class
        );
        
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    /**
     * Obtiene la instancia singleton del gestor de jugadores.
     * @return La instancia del gestor
     */
    public static PlayerManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }

    /**
     * Obtiene el gestor de música para un servidor específico.
     * @param guild El servidor
     * @return El gestor de música
     */
    @SuppressWarnings("unused")
    public GuildMusicManager getMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    /**
     * Carga y reproduce una pista desde una URL.
     * @param channel El canal de texto donde enviar mensajes
     * @param trackUrl La URL de la pista (YouTube, SoundCloud, etc.)
     */
    public void loadAndPlay(TextChannel channel, String trackUrl) {
        final GuildMusicManager musicManager = this.getMusicManager(channel.getGuild());

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.getScheduler().queue(track);
                
                String message = "🎵 **Añadido a la cola:**\n" +
                        "`" + track.getInfo().title + "`" +
                        " por `" + track.getInfo().author + "`";
                
                channel.sendMessage(message).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getSelectedTrack() != null) {
                    trackLoaded(playlist.getSelectedTrack());
                } else if (playlist.isSearchResult()) {
                    trackLoaded(playlist.getTracks().get(0));
                } else {
                    // Cargar toda la playlist
                    for (AudioTrack track : playlist.getTracks()) {
                        musicManager.getScheduler().queue(track);
                    }
                    
                    String message = "🎵 **Playlist añadida:**\n" +
                            "`" + playlist.getName() + "`" +
                            " (" + playlist.getTracks().size() + " canciones)";
                    
                    channel.sendMessage(message).queue();
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("❌ **No se encontró nada con:** `" + trackUrl + "`").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("❌ **Error al cargar la pista:** " + exception.getMessage()).queue();
                exception.printStackTrace();
            }
        });
    }
}