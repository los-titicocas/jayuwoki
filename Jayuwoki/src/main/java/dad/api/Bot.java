package dad.api;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class Bot {
    public static void main(String[] args) throws Exception {
        // Leer el token desde la variable de entorno
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("El token del bot no está configurado como variable de entorno.");
        }

        // Inicializar el bot con el token
        JDA jda = JDABuilder.createDefault(token).build();
    }
}
