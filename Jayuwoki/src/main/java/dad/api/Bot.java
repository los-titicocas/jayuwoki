package dad.api;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

public class Bot {

    private static JDA jda;
    private final BooleanProperty isConnected = new SimpleBooleanProperty();

    public void startConnection() {
        // Leer el token desde la variable de entorno
        String token = System.getenv("BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Token is not set as an environment variable");
        }

        // Inicializar el bot con el token
        jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS) // Intends so the Bot can see members and messages from the guild
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        jda.addEventListener(new EventoPrueba());
        isConnected.set(true);
    }

    public void stopConnection() {
        // Detener la conexión
        if (jda != null) {
            jda.shutdown();
            isConnected.set(false);
        }
    }

    public boolean isIsconnected() {
        return isConnected.get();
    }

    public BooleanProperty isconnectedProperty() {
        return isConnected;
    }
}
