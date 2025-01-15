package dad.api;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Bot {

    private static JDA jda;
    private boolean connected = false;

    public void startConnection() {
        // Leer el token desde la variable de entorno
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Token is not set as an environment variable");
        }

        // Inicializar el bot con el token
        jda = JDABuilder.createDefault(token).build();
    }

    public void stopConnection() {
        // Detener la conexión
        jda.shutdown();
    }

    public boolean isConnected() {
        return connected;
    }
}
