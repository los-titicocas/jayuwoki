package dad.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class DBManager {

    // Attributes
    private Firestore db;
    private MessageReceivedEvent event;
    private String currentServer;

    public DBManager() {
        try {
            // Cargar el archivo JSON desde la carpeta resources
            InputStream serviceAccount = pruebaDB.class.getClassLoader()
                    .getResourceAsStream("jayuwokidb-firebase-adminsdk-4z17f-f9bfa41da2.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Archivo de credenciales no encontrado en resources");
            }

            // Configura Firebase
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://JayuwokiDB.firebaseio.com") // Reemplaza con tu URL
                    .build();

            FirebaseApp.initializeApp(options);

            // Obtener una instancia de Firestore
            db = FirestoreClient.getFirestore();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void AddPlayer(Player newPlayer) {
        try {

            if (!CheckPlayerFound(newPlayer)) {
                event.getChannel().sendMessage("El jugador ya está en la base de datos").queue();
                return;
            }

            // Parce the name of the server to remove special characters and spaces
            CollectionReference playersCollection = db.collection(currentServer)
                    .document("Privadita")
                    .collection("Players");

            playersCollection.document(newPlayer.getName()).set(newPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void AddPlayers(List<Player> newPlayers) {
        try {
            System.out.println(currentServer);
            List<Player> playersNotInDB = GetPlayersNotFound(newPlayers);

            // Si todos los jugadores ya están en la base de datos, solo enviamos un mensaje
            if (playersNotInDB.isEmpty()) {
                // Jugadores que ya están en la base de datos
                List<Player> playersAlreadyInDB = newPlayers.stream()
                        .filter(player -> !playersNotInDB.contains(player))
                        .collect(Collectors.toList());

                StringBuilder message = new StringBuilder("Jugadores ya en la base de datos:\n");
                for (Player player : playersAlreadyInDB) {
                    System.out.println("- " + player.getName());
                    message.append("- ").append(player.getName()).append("\n");
                }

                // Enviar un único mensaje al canal de Discord
                event.getChannel().sendMessage(message.toString().trim()).queue();
                return; // Aquí se puede continuar ya que no es necesario agregar jugadores si ya están en la base de datos
            }

            // Si hay jugadores que no están en la base de datos, se agregan
            CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");
            WriteBatch batch = db.batch();

            // Agregar cada jugador que no esté en la base de datos
            for (Player player : playersNotInDB) {
                DocumentReference playerDoc = playersCollection.document(player.getName());
                batch.set(playerDoc, player);
            }

            // Ejecutar la operación de batch para agregar todos los jugadores a la vez
            batch.commit().get();

            // Mensaje en Discord indicando que los jugadores fueron añadidos
            event.getChannel().sendMessage("Jugadores añadidos exitosamente a la base de datoss.").queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Check if the players are in the database (List type)
    public List<Player> GetPlayersNotFound(List<Player> players) {
        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        // Get the name of each player to search it
        List<String> playerNames = players.stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        try {
            // Create the query to search the 10 player names
            QuerySnapshot querySnapshot = playersCollection
                    .whereIn("name", playerNames)
                    .get()
                    .get();

            // List of the player names found in the database
            List<String> foundPlayerNames = querySnapshot.getDocuments().stream()
                    .map(doc -> doc.getString("name"))
                    .collect(Collectors.toList());

            // Return the players that are not in the database
            return players.stream()
                    .filter(player -> !foundPlayerNames.contains(player.getName()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return players; // Si hay un error, asumimos que ninguno está en la base de datos
        }
    }

    // Check if the player is in the database (individual type)
    public boolean CheckPlayerFound(Player player) {

        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        try {
            // Create the query to search the player name
            QuerySnapshot querySnapshot = playersCollection
                    .whereEqualTo("name", player.getName())
                    .get()
                    .get();

            // Return true if the player is not in the database
            return querySnapshot.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
            return true; // Si hay un error, asumimos que el jugador no está en la base de datos
        }

    }
    public Firestore getDb() {
        return db;
    }

    public void setEvent(MessageReceivedEvent event) {
        this.event = event;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }
}
