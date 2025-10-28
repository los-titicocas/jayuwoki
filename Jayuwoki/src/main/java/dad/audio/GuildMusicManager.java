package dad.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

/**
 * Gestiona el audio para un servidor específico de Discord.
 * Contiene el reproductor de audio y el planificador de pistas.
 */
public class GuildMusicManager {
    
    private final AudioPlayer player;
    private final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;

    /**
     * Crea un gestor de música para un servidor.
     * @param manager El gestor de reproductores de audio
     */
    public GuildMusicManager(AudioPlayerManager manager) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.player);
        this.player.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.player);
    }

    /**
     * @return El manejador de envío de audio para Discord
     */
    public AudioPlayerSendHandler getSendHandler() {
        return sendHandler;
    }

    /**
     * @return El reproductor de audio
     */
    public AudioPlayer getPlayer() {
        return player;
    }

    /**
     * @return El planificador de pistas
     */
    public TrackScheduler getScheduler() {
        return scheduler;
    }
}