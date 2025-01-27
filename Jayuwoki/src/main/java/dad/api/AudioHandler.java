package dad.api;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
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

    public void loadAndPlay(String filePath) {
        playerManager.loadItem(filePath, new AudioLoadResultHandler() {
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
                System.out.println("No se encontró la pista: " + filePath);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.err.println("Error al cargar la pista: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        player.stopTrack();
    }

    public void pause() {
        player.setPaused(true);
    }

    public void resume() {
        player.setPaused(false);
    }

    public boolean isPaused() {
        return player.isPaused();
    }
}