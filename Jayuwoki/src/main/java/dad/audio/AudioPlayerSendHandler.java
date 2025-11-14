package dad.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

/**
 * Envía audio desde LavaPlayer a Discord.
 */
public class AudioPlayerSendHandler implements AudioSendHandler {
    
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        return audioPlayer.provide(frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        // Flip the buffer to prepare for reading.
        buffer.flip();

        // Copy the valid bytes into a new array-backed ByteBuffer because
        // JDA requires a ByteBuffer with a backing array (hasArray()==true).
        int length = buffer.remaining();
        byte[] data = new byte[length];
        buffer.get(data);

        // Clear the original buffer so MutableAudioFrame can reuse it.
        buffer.clear();

        // Return an array-backed ByteBuffer containing only the valid bytes.
        return ByteBuffer.wrap(data);
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}