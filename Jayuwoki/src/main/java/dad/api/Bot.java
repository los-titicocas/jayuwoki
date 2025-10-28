package dad.api;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Bot {

    private static JDA jda;
    private final BooleanProperty isConnected = new SimpleBooleanProperty();
    // Instance of the command class needed to bind the commands to the log
    private final Commands commands;

    public Bot() {
        isConnected.set(false);

        // reinitialize the commands
        commands = new Commands();
        commands.setJda(jda);
    }

    public Commands getCommands() {
        return commands;
    }

    public void startConnection() throws IOException {

        try (InputStream inputStream = Bot.class.getClassLoader().getResourceAsStream("api.config")) {
            if (inputStream == null) {
                System.out.println("No se encontró el archivo");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String token = reader.readLine();

            // Inicializar el bot con el token
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS) // Intends so the Bot can see members and messages from the guild
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();

            commands.setJda(jda);

            jda.addEventListener(commands);
            isConnected.set(true);

        }
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

    public static void main(String[] args) throws IOException {
        Bot bot = new Bot();
        bot.startConnection();
    }
}

