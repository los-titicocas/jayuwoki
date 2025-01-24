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

    public void AddPlayer(Player newPlayer, MessageReceivedEvent event) {
        try {
            // Get the name of the server from the event
            String serverName = event.getGuild().getName();

            // Parce the name of the server to remove special characters and spaces
            serverName = serverName.replaceAll("[^a-zA-Z0-9]", "_");

            CollectionReference playersCollection = db.collection(serverName)
                    .document("Players")
                    .collection("Players");

            playersCollection.document(newPlayer.getName()).set(newPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void AddPlayers(List<Player> newPlayers, MessageReceivedEvent event) {
        try {
            // Get the name of the server from the event
            String serverName = event.getGuild().getName();

            // Parce the name of the server to remove special characters and spaces
            serverName = serverName.replaceAll("[^a-zA-Z0-9]", "_");

            // Create a batch to commit all the players at once
            CollectionReference playersCollection = db.collection(serverName).document("Players").collection("Players");
            WriteBatch batch = db.batch();

            // add each player to the batch
            for (Player player : newPlayers) {
                DocumentReference playerDoc = playersCollection.document(player.getName());
                batch.set(playerDoc, player);
            }

            // Execute the batch
            batch.commit().get(); // Llamada bloqueante para esperar que se complete

            System.out.println("Jugadores añadidos exitosamente al servidor: " + serverName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Player> GetPlayersNotFound(List<Player> players) {
        CollectionReference playersCollection = db.collection("Players");

        // Get the name of each player to search it
        List<String> playerNames = players.stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        try {
            // Create the query to search the 10 player names
            QuerySnapshot querySnapshot = playersCollection
                    .whereIn("name", playerNames) // Cambia "name" si el campo es diferente
                    .get()
                    .get();

            // List of the player names found in the database
            List<String> foundPlayerNames = querySnapshot.getDocuments().stream()
                    .map(doc -> doc.getString("name")) // Cambia "name" según el campo en tu base de datos
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

    public Firestore getDb() {
        return db;
    }

}
