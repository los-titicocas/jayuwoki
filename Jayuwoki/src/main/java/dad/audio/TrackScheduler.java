package dad.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Gestiona la cola de reproducción y el orden de las canciones.
 * Se encarga de pasar automáticamente a la siguiente canción cuando una termina.
 */
public class TrackScheduler extends AudioEventAdapter {
    
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Añade una canción a la cola.
     * Si no hay nada reproduciéndose, inicia la reproducción inmediatamente.
     * 
     * @param track La canción a añadir
     */
    public void queue(AudioTrack track) {
        // ✅ CORRECCIÓN: Si no hay nada reproduciéndose, iniciar inmediatamente
        if (!player.startTrack(track, true)) {
            // Si startTrack devuelve false, significa que ya hay algo reproduciéndose
            // Entonces añadimos a la cola
            queue.offer(track);
            System.out.println("🎵 Canción añadida a la cola: " + track.getInfo().title);
        } else {
            // Si startTrack devuelve true, la canción empezó a reproducirse
            System.out.println("▶️ Reproduciendo: " + track.getInfo().title);
        }
    }

    /**
     * Pasa a la siguiente canción en la cola.
     */
    public void nextTrack() {
        AudioTrack nextTrack = queue.poll();
        if (nextTrack != null) {
            player.startTrack(nextTrack, false);
            System.out.println("⏭️ Siguiente canción: " + nextTrack.getInfo().title);
        } else {
            System.out.println("⏹️ Cola vacía, reproducción detenida");
        }
    }

    /**
     * Se ejecuta cuando una canción termina de reproducirse.
     */
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        System.out.println("✅ Canción terminada: " + track.getInfo().title + " | Razón: " + endReason);
        
        // Solo pasa a la siguiente canción si terminó normalmente o fue detenida
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    /**
     * Se ejecuta cuando empieza a reproducirse una canción.
     */
    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        System.out.println("🎵 Iniciando reproducción: " + track.getInfo().title);
    }

    /**
     * Obtiene la cola de reproducción.
     */
    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }
}