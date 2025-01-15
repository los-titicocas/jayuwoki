package dad.api;

import dad.api.models.Player;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.Arrays;


public class Commands extends ListenerAdapter {

    private String[] roles = {"Top", "Jungla", "Mid", "Adc", "Support"};
    private ObservableList<Player> players = new SimpleListProperty<>(FXCollections.observableArrayList());



    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Get the menssage content
        String message = event.getMessage().getContentRaw().toLowerCase();

        if (message.startsWith("$")) {
            String[] comando = message.split(" ");
            // Switch with all the posible commands
            switch (message) {
                case "$privadita":
                    // Check if the command has the correct number of players
                    if (comando.length == 11) {
                        StartPrivadita(comando);
                    } else {
                        event.getChannel().sendMessage("El comando $privadita necesita 10 jugadores").queue();
                    }

            }
        }
    }

    private void StartPrivadita(String[] comando) {
        // int[] newArray = Arrays.copyOfRange(originalArray, 1, originalArray.length);
        String[] playersNames = Arrays.copyOfRange(comando, 1, comando.length);
        for (String name : playersNames) {
            Player player = new Player();
            player.setName(name);
            players.add(player);
        }

        ObservableList<String> blueTeam = new SimpleListProperty<>(FXCollections.observableArrayList());
        ObservableList<String> redTeam = new SimpleListProperty<>(FXCollections.observableArrayList());

        // Get a random number between 0 to 4 to select the role
        int rolsLeft = 5;
        for (int i = 0; i < 5; i++) {
            int randomRole = (int) (Math.random() * rolsLeft);
            // Check if the role is already taken
            if (roles[randomRole] != null) {
                blueTeam.add(players.get(i) + " " + roles[randomRole]);
                roles[randomRole] = null;
            } else {
                i--;
            }
        }


    }
}
