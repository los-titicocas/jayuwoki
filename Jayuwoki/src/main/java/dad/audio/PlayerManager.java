package dad.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestiona todos los reproductores de música para todos los servidores.
 */
public class PlayerManager {
    
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    private PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();

        // ✅ YouTube Source (v1.8.3 - configuración simplificada)
        YoutubeAudioSourceManager youtubeSource = new YoutubeAudioSourceManager(
            true  // allowSearch = permite búsquedas por nombre
        );
        
        this.playerManager.registerSourceManager(youtubeSource);

        // ✅ Registrar todas las demás fuentes (SoundCloud, Bandcamp, etc.)
        // IMPORTANTE: Excluir YouTube porque ya lo registramos arriba
        AudioSourceManagers.registerRemoteSources(
            this.playerManager, 
            com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class
        );
        
        AudioSourceManagers.registerLocalSource(this.playerManager);
        
        System.out.println("✅ PlayerManager inicializado:");
        System.out.println("   - YouTube (v2 plugin 1.8.3)");
        System.out.println("   - SoundCloud");
        System.out.println("   - Bandcamp");
        System.out.println("   - Vimeo");
        System.out.println("   - Twitch");
        System.out.println("   - HTTP URLs");
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
        return musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            GuildMusicManager manager = new GuildMusicManager(this.playerManager);
            guild.getAudioManager().setSendingHandler(manager.getSendHandler());
            
            System.out.println("✅ MusicManager creado para servidor: " + guild.getName() + " (" + guild.getId() + ")");
            
            return manager;
        });
    }

    /**
     * Carga y reproduce una pista desde una URL.
     * @param channel El canal de texto donde enviar mensajes
     * @param trackUrl La URL de la pista (YouTube, SoundCloud, etc.)
     */
    public void loadAndPlay(TextChannel channel, String trackUrl) {
        GuildMusicManager musicManager = this.getMusicManager(channel.getGuild());

        this.playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                System.out.println("📥 Track cargado: " + track.getInfo().title);
                
                channel.sendMessage("✅ **Añadido a la cola:** `" + track.getInfo().title + "`").queue();
                musicManager.getScheduler().queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<AudioTrack> tracks = playlist.getTracks();
                
                if (playlist.isSearchResult()) {
                    // Es una búsqueda, tomar el primer resultado
                    AudioTrack firstTrack = tracks.get(0);
                    channel.sendMessage("✅ **Añadido a la cola:** `" + firstTrack.getInfo().title + "`").queue();
                    musicManager.getScheduler().queue(firstTrack);
                    System.out.println("✅ Track de búsqueda cargado: " + firstTrack.getInfo().title);
                } else {
                    // Es una playlist completa
                    channel.sendMessage("✅ **Añadida playlist:** `" + playlist.getName() + 
                                      "` (" + tracks.size() + " canciones)").queue();
                    for (AudioTrack track : tracks) {
                        musicManager.getScheduler().queue(track);
                    }
                    System.out.println("✅ Playlist cargada: " + playlist.getName() + " con " + tracks.size() + " tracks");
                }
            }

            @Override
            public void noMatches() {
                channel.sendMessage("❌ **No se encontró nada con:** `" + trackUrl + "`\n" +
                                  "💡 **Prueba con:**\n" +
                                  "• Una URL directa de YouTube/SoundCloud\n" +
                                  "• Buscar por nombre (sin URL)").queue();
                System.out.println("❌ No matches para: " + trackUrl);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                String errorMsg = exception.getMessage();
                
                // ✅ Mensaje de error mejorado para bloqueos de YouTube
                if (errorMsg.contains("This video cannot be loaded") || 
                    errorMsg.contains("action functions") ||
                    errorMsg.contains("Must find") ||
                    errorMsg.contains("cipher")) {
                    
                    channel.sendMessage("❌ **YouTube bloqueó este video.**\n" +
                                      "⚠️ **Posibles soluciones:**\n" +
                                      "1️⃣ Intenta con otra canción de YouTube\n" +
                                      "2️⃣ Usa SoundCloud: `$play https://soundcloud.com/...`\n" +
                                      "3️⃣ Busca por nombre en lugar de URL\n" +
                                      "4️⃣ Algunos videos están protegidos por YouTube").queue();
                    
                    System.err.println("⚠️ YouTube bloqueó: " + trackUrl);
                    System.err.println("   Razón: " + errorMsg);
                } else {
                    channel.sendMessage("❌ **Error al cargar:** `" + errorMsg + "`\n" +
                                      "💡 Verifica que la URL sea correcta.").queue();
                    System.err.println("❌ Error al cargar: " + errorMsg);
                }
                
                exception.printStackTrace();
            }
        });
    }
}