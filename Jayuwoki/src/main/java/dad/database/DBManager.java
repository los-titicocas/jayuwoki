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
                    .getResourceAsStream("jayuwokidb-firebase-adminsdk.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Archivo de credenciales no encontrado en resources");
            }

            // Configura Firebase
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://JayuwokiDB.firebaseio.com")
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

            List<Player> playersAlreadyInDB = newPlayers.stream()
                    .filter(player -> !playersNotInDB.contains(player))
                    .collect(Collectors.toList());

            if (!playersAlreadyInDB.isEmpty()) {
                StringBuilder message = new StringBuilder("Los siguientes jugadores ya están en la base de datos:\n");
                for (Player player : playersAlreadyInDB) {
                    System.out.println("- " + player.getName());
                    message.append("- ").append(player.getName()).append("\n");
                }
                event.getChannel().sendMessage(message.toString().trim()).queue();
            }

            if (playersNotInDB.isEmpty()) {
                event.getChannel().sendMessage("Todos los jugadores ya están en la base de datos. No se añaden nuevos jugadores.").queue();
                return;
            }

            StringBuilder addMessage = new StringBuilder("Los siguientes jugadores se van a añadir a la base de datos:\n");
            CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");
            WriteBatch batch = db.batch();

            for (Player player : playersNotInDB) {
                DocumentReference playerDoc = playersCollection.document(player.getName());
                batch.set(playerDoc, player);
                addMessage.append("- ").append(player.getName()).append("\n");
            }

            batch.commit().get();

            event.getChannel().sendMessage(addMessage.toString().trim()).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ShowPlayerElo(String name) {
        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        try {
            DocumentSnapshot playerDoc = playersCollection.document(name).get().get();

            if (playerDoc.exists()) {
                Player player = playerDoc.toObject(Player.class);
                event.getChannel().sendMessage(player.PrintStats()).queue();
            } else {
                event.getChannel().sendMessage("El jugador no está en la base de datos").queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ShowAllElo() {
        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        try {
            QuerySnapshot querySnapshot = playersCollection.get().get();

            StringBuilder message = new StringBuilder("```");
            for (DocumentSnapshot playerDoc : querySnapshot.getDocuments()) {
                Player player = playerDoc.toObject(Player.class);
                message.append(player.PrintStats()).append("\n");
            }
            message.append("```");

            event.getChannel().sendMessage(message.toString()).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void DeletePlayer(String name) {
        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        try {
            DocumentSnapshot playerDoc = playersCollection.document(name).get().get();

            if (playerDoc.exists()) {
                playersCollection.document(name).delete();
                event.getChannel().sendMessage("El jugador ha sido eliminado de la base de datos").queue();
            } else {
                event.getChannel().sendMessage("El jugador no está en la base de datos").queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean CheckPlayerFound(Player player) {
        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        try {
            QuerySnapshot querySnapshot = playersCollection
                    .whereEqualTo("name", player.getName())
                    .get()
                    .get();

            return querySnapshot.isEmpty();

        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public List<Player> GetPlayersNotFound(List<Player> players) {
        CollectionReference playersCollection = db.collection(currentServer).document("Privadita").collection("Players");

        List<String> playerNames = players.stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        try {
            QuerySnapshot querySnapshot = playersCollection
                    .whereIn("name", playerNames)
                    .get()
                    .get();

            List<String> foundPlayerNames = querySnapshot.getDocuments().stream()
                    .map(doc -> doc.getString("name"))
                    .collect(Collectors.toList());

            return players.stream()
                    .filter(player -> !foundPlayerNames.contains(player.getName()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return players;
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