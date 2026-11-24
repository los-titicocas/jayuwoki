package dad.season;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Comprueba en segundo plano, en todos los servidores, si ha terminado la temporada.
 *
 * No se programa un temporizador para "el día X": si el bot estuviera apagado ese día se perdería. En su
 * lugar se compara la fecha con la temporada guardada nada más conectar y luego cada pocos minutos. Como la
 * transición está protegida por el estado en Firestore, repetir la comprobación es inofensivo.
 */
public class SeasonScheduler {

    private static final long INITIAL_DELAY_SECONDS = 20;   // respaldo: lo normal es checkNow() al recibir ReadyEvent
    private static final long INTERVAL_SECONDS = 5 * 60;

    private final SeasonManager seasons;
    private final Supplier<JDA> jda;
    private ScheduledExecutorService executor;

    public SeasonScheduler(SeasonManager seasons, Supplier<JDA> jda) {
        this.seasons = seasons;
        this.jda = jda;
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "season-scheduler");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::tick, INITIAL_DELAY_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
        System.out.println("⏰ [Temporadas] Comprobación automática activa (cada " + INTERVAL_SECONDS / 60
                + " min, zona " + SeasonCalendar.zone() + ")");
    }

    /** Lanza una comprobación inmediata (p. ej. al conectar con Discord), en el hilo del planificador. */
    public synchronized void checkNow() {
        if (executor != null) {
            executor.execute(this::tick);
        }
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Nunca lanza: una excepción cancelaría todas las ejecuciones siguientes del ScheduledExecutorService. */
    void tick() {
        try {
            JDA current = jda.get();
            if (current == null || current.getStatus() != JDA.Status.CONNECTED) {
                return; // se reintenta en el siguiente ciclo
            }
            for (Guild guild : current.getGuilds()) {
                try {
                    seasons.ensureCurrent(guild.getId());
                } catch (Throwable t) {
                    System.err.println("❌ [Temporadas] Error comprobando " + guild.getName() + " (" + guild.getId()
                            + "), se reintentará: " + t);
                    t.printStackTrace();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
