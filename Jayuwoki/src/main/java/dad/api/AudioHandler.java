package dad.api.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioLoadResult;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dad.api.GuildAudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;

public class AudioHandler {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final GuildAudioSendHandler sendHandler;

    public AudioHandler(Guild guild) {
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        player = playerManager.createPlayer();
        sendHandler = new GuildAudioSendHandler(player);

        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(sendHandler);
    }

    public void loadAndPlay(String trackUrl) {
        playerManager.loadItem(trackUrl, new AudioLoadResult() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                player.playTrack(playlist.getTracks().get(0)); // Reproduce la primera pista
            }

            @Override
            public void noMatches() {
                System.out.println("No se encontró la pista: " + trackUrl);
            }

            @Override
            public void loadFailed(Exception exception) {
                exception.printStackTrace();
            }
        });
    }
}