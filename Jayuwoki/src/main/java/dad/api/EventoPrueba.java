package dad.api;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventoPrueba extends ListenerAdapter {

    public void PoloEvento(MessageReceivedEvent  event) {
        if (event.getMessage().getContentRaw().equals("Marco")) {
            event.getChannel().sendMessage("Polo").queue();
        }
    }
}
