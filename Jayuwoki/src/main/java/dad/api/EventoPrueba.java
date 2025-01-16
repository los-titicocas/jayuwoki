package dad.api;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;

public class EventoPrueba extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw().toLowerCase();
        System.out.println("Mensaje recibido: " + message);

        if (message.equals("marco")) {
            event.getChannel().sendMessage("Polo").queue();
        } else if (message.equals("hola")) {
            event.getChannel().sendMessage("En tu culo mi aparato").queue();
        } else if (message.contains("razón")) {
            event.getChannel().sendMessage("Que si , Que si").queue();
        } else if (message.equals("mike")) {
            event.getChannel().sendMessage("sáquelo a pasear").queue();
        } else if (message.contains("5")) {
            InputStream resource = getClass().getResourceAsStream("/images/vegeta.jpg");
            if (resource == null) {
                System.out.println("Resource not found: /images/vegeta.jpg");
            } else {
                event.getChannel().sendFiles(FileUpload.fromData(resource, "vegeta.jpg")).queue();
            }
        }
    }
}

