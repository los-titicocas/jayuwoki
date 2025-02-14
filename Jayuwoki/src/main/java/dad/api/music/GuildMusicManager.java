package dad.api.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

public class GuildMusicManager {
    private final TrackScheduler scheduler;
    private final AudioForwarder audioForwarder;

    public GuildMusicManager(AudioPlayerManager manager) {
        AudioPlayer player = manager.createPlayer();
        this.scheduler = new TrackScheduler(player);
        this.audioForwarder = new AudioForwarder(player);
        player.addListener(scheduler);
    }

    public TrackScheduler getScheduler() {
        return scheduler;
    }

    public AudioForwarder getAudioForwarder() {
        return audioForwarder;
    }
}
