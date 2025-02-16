package dad.api.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import java.nio.ByteBuffer;

public class AudioForwarder {
    private final AudioPlayer player;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;

    public AudioForwarder(AudioPlayer player) {
        this.player = player;
        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
    }

    public boolean canProvide() {
        return player.provide(frame);
    }

    public ByteBuffer provide20MsAudio() {
        return buffer.flip();
    }

    public boolean isOpus() {
        return true;
    }
}
