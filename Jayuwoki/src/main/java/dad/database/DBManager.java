package dad.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import dad.utils.Utils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DBManager {

    // Attributes
    private Firestore db;
    private MessageReceivedEvent event;
    private Member discordUser;
    private StringProperty currentServer = new SimpleStringProperty();
    private BooleanProperty openPermissions = new SimpleBooleanProperty();

    public DBManager() {
        try {
            // Cargar el archivo JSON desde la carpeta resources
            InputStream serviceAccount = DBManager.class.getClassLoader()
                    .getResourceAsStream("jayuwokidb-firebase-adminsdk.json");

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

    /**
     * Obtiene jugadores desde Firebase por sus nombres.
     * Si algún jugador tiene Elo = 0, lo inicializa automáticamente a 1000 y lo persiste en Firebase.
     * 
     * @param nombres Array con los nombres de los jugadores a buscar
     * @param event Evento de Discord para enviar mensajes
     * @return Lista de jugadores encontrados con sus datos actualizados
     */
    public List<Player> GetPlayers(String[] nombres, MessageReceivedEvent event) {
        List<Player> players = new ArrayList<>();
        CollectionReference playersCollection = db.collection(currentServer.get())
                .document("Privadita")
                .collection("Players");

        try {
            QuerySnapshot querySnapshot = playersCollection
                    .whereIn("name", java.util.Arrays.asList(nombres))
                    .get()
                    .get();

            WriteBatch batch = db.batch();
            boolean hasDefaultEloUpdates = false;

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Player player = doc.toObject(Player.class);
                if (player != null) {
                    // ✨ NUEVA LÓGICA: Solo corregir Elos anómalos por BUGS
                    // Un jugador tiene Elo anómalo SI:
                    // 1. Tiene Elo < 800 Y
                    // 2. (Tiene 0 partidas jugadas O tiene pocas partidas donde es matemáticamente imposible)
                    
                    int totalGames = player.getWins() + player.getLosses();
                    boolean hasAnomalousElo = false;
                    
                    // Caso 1: Elo bajo sin partidas (bug de inicialización)
                    if (player.getElo() < 800 && totalGames == 0) {
                        hasAnomalousElo = true;
                        System.out.println("⚠️ BUG DETECTADO: Jugador '" + player.getName() + 
                                         "' tiene Elo = " + player.getElo() + " pero 0 partidas. " +
                                         "Reiniciando a 1000...");
                    }
                    // Caso 2: Elo imposible matemáticamente (ej: 29 con 1-1 record)
                    // Con pocas partidas (≤3), es imposible bajar de 800 legítimamente
                    // Peor caso: 3 derrotas seguidas contra Elo 2000 = 1000 - 32 - 32 - 32 = 904
                    else if (player.getElo() < 800 && totalGames > 0 && totalGames <= 3) {
                        hasAnomalousElo = true;
                        System.out.println("⚠️ BUG DETECTADO: Jugador '" + player.getName() + 
                                         "' tiene Elo = " + player.getElo() + 
                                         " con solo " + totalGames + " partidas. " +
                                         "Esto es imposible matemáticamente. Reiniciando a 1000...");
                    }
                    // Caso 3: Elo muy bajo pero legítimo (muchas derrotas)
                    else if (player.getElo() < 800 && totalGames > 3) {
                        // Este es un jugador legítimamente malo, NO corregir
                        System.out.println("ℹ️ INFO: Jugador '" + player.getName() + 
                                         "' tiene Elo bajo (" + player.getElo() + 
                                         ") pero es legítimo (W:" + player.getWins() + 
                                         " L:" + player.getLosses() + ")");
                    }
                    
                    // Solo resetear si es un bug confirmado
                    if (hasAnomalousElo) {
                        player.setElo(1000);
                        player.setWins(0);
                        player.setLosses(0);
                        
                        DocumentReference playerDoc = playersCollection.document(player.getName());
                        batch.set(playerDoc, player);
                        hasDefaultEloUpdates = true;
                    }
                    
                    players.add(player);
                }
            }

            if (hasDefaultEloUpdates) {
                try {
                    batch.commit().get();
                    System.out.println("✅ Bugs de Elo corregidos y guardados en Firebase");
                } catch (Exception be) {
                    System.out.println("⚠️ Error al guardar correcciones: " + be.getMessage());
                    be.printStackTrace();
                }
            }

            // Validar jugadores no encontrados
            List<String> jugadoresEncontrados = players.stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());

            List<String> jugadoresNoEncontrados = java.util.Arrays.stream(nombres)
                    .filter(nombre -> !jugadoresEncontrados.contains(nombre))
                    .collect(Collectors.toList());

            if (!jugadoresNoEncontrados.isEmpty()) {
                StringBuilder message = new StringBuilder("Los siguientes jugadores no están en la base de datos:\n");
                for (String nombre : jugadoresNoEncontrados) {
                    message.append("- ").append(nombre).append("\n");
                }
                event.getChannel().sendMessage(message.toString().trim()).queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return players;
    }

    /**
     * Actualiza la lista de jugadores en Firebase.
     * Este método se llama después de una privadita para guardar los cambios de Elo, wins y losses.
     * 
     * @param server Nombre del servidor
     * @param players Lista de jugadores con datos actualizados
     */
    public void updatePlayers(String server, javafx.collections.ObservableList<Player> players) {
        System.out.println("Actualizando jugadores en el servidor: " + server);

        CollectionReference playersCollection = db.collection(server)
                .document("Privadita")
                .collection("Players");

        WriteBatch batch = db.batch();

        for (Player player : players) {
            System.out.println("Actualizando jugador: " + player.getName() + 
                             " - Elo: " + player.getElo() + 
                             " - Wins: " + player.getWins() + 
                             " - Losses: " + player.getLosses());

            DocumentReference playerDoc = playersCollection.document(player.getName());
            batch.set(playerDoc, player);
        }

        try {
            batch.commit().get();  // Se espera a que termine la operación
            System.out.println("✅ Jugadores actualizados correctamente en Firebase.");
        } catch (Exception e) {
            System.out.println("❌ Error al actualizar jugadores: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void AddPlayer(Player newPlayer) {
        try {
            openPermissions.set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
            if (CheckPermissions(openPermissions.get())) {
                if (!CheckPlayerFound(newPlayer)) {
                    event.getChannel().sendMessage("El jugador ya está en la base de datos").queue();
                    return;
                }
                // Parce the name of the server to remove special characters and spaces
                CollectionReference playersCollection = db.collection(currentServer.get())
                        .document("Privadita")
                        .collection("Players");

                playersCollection.document(newPlayer.getName()).set(newPlayer);
                event.getChannel().sendMessage("Jugador " + newPlayer.getName() + " añadido a la base de datos").queue();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public void AddPlayers(List<Player> newPlayers) {
        openPermissions.set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        if (CheckPermissions(openPermissions.get())) {
            try {
                // Obtener los jugadores que no están en la base de datos
                List<Player> playersNotInDB = GetPlayersNotFound(newPlayers);

                // Delete the players that are already in the database
                List<Player> playersAlreadyInDB = newPlayers.stream()
                        .filter(player -> !playersNotInDB.contains(player))
                        .collect(Collectors.toList());

                // Show the players that are already in the database
                if (!playersAlreadyInDB.isEmpty()) {
                    StringBuilder message = new StringBuilder("Los siguientes jugadores ya están en la base de datos:\n");
                    for (Player player : playersAlreadyInDB) {
                        System.out.println("- " + player.getName());
                        message.append("- ").append(player.getName()).append("\n");
                    }
                    event.getChannel().sendMessage(message.toString().trim()).queue();
                }

                // Message if everyone is already in the database
                if (playersNotInDB.isEmpty()) {
                    event.getChannel().sendMessage("Todos los jugadores ya están en la base de datos. No se añaden nuevos jugadores.").queue();
                    return;
                }

                // Message if there are players to add and add them
                StringBuilder addMessage = new StringBuilder("Los siguientes jugadores se van a añadir a la base de datos:\n");
                CollectionReference playersCollection = db.collection(currentServer.get()).document("Privadita").collection("Players");
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
    }

    public void DeletePlayer(String name) {
        openPermissions.set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        if (CheckPermissions(openPermissions.get())) {
            CollectionReference playersCollection = db.collection(currentServer.get()).document("Privadita").collection("Players");

            try {
                // Get the player from the database
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
    }

    public void ShowPlayerElo(String name) {
        CollectionReference playersCollection = db.collection(currentServer.get()).document("Privadita").collection("Players");

        try {
            // Get the player from the database
            DocumentSnapshot playerDoc = playersCollection.document(name).get().get();

            if (playerDoc.exists()) {
                Player player = playerDoc.toObject(Player.class);
                // Show all the player stats
                event.getChannel().sendMessage(player.PrintStats()).queue();
            } else {
                event.getChannel().sendMessage("El jugador no está en la base de datos").queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ShowAllElo() {
        CollectionReference playersCollection = db.collection(currentServer.get()).document("Privadita").collection("Players");

        try {
            // Get all the players from the database
            QuerySnapshot querySnapshot = playersCollection.get().get();

            // Show the stats of all the players
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

    // Check if the player is in the database (individual type)
    public boolean CheckPlayerFound(Player player) {

        CollectionReference playersCollection = db.collection(currentServer.get()).document("Privadita").collection("Players");

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

    // Check if the players are in the database (List type)
    public List<Player> GetPlayersNotFound(List<Player> players) {
        CollectionReference playersCollection = db.collection(currentServer.get()).document("Privadita").collection("Players");

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

    // Check if the user has administrator permissions, so he will be able to modify the database
    private boolean CheckPermissions(Boolean openPermissions) {
        if (openPermissions) {
            return true;
        } else if (discordUser.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        } else {
            event.getChannel().sendMessage("No tienes permisos para modificar la base de datos").queue();
            return false;
        }

    }

    public Firestore getDb() {
        return db;
    }

    public void setEvent(MessageReceivedEvent event) {
        this.event = event;
        this.discordUser = event.getMember();
    }

    public String getCurrentServer() {
        return currentServer.get();
    }

    public StringProperty currentServerProperty() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer.set(currentServer);
    }

    public boolean isOpenPermissions() {
        return openPermissions.get();
    }

    public BooleanProperty openPermissionsProperty() {
        return openPermissions;
    }

    public void setOpenPermissions(boolean openPermissions) {
        this.openPermissions.set(openPermissions);
    }

    /**
     * Permite a un admin resetear manualmente el Elo de un jugador.
     * Útil para segundas oportunidades o casos especiales.
     */
    public void AdminResetPlayerElo(String name) {
        openPermissions.set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        if (CheckPermissions(openPermissions.get())) {
            CollectionReference playersCollection = db.collection(currentServer.get())
                    .document("Privadita")
                    .collection("Players");

            try {
                DocumentSnapshot playerDoc = playersCollection.document(name).get().get();

                if (playerDoc.exists()) {
                    Player player = playerDoc.toObject(Player.class);
                    int oldElo = player.getElo();
                    int oldWins = player.getWins();
                    int oldLosses = player.getLosses();
                    
                    player.setElo(1000);
                    player.setWins(0);
                    player.setLosses(0);
                    
                    playersCollection.document(name).set(player);
                    
                    event.getChannel().sendMessage(
                        "✅ **" + name + "** ha sido reseteado manualmente por un admin:\n" +
                        "```\n" +
                        "Antes:  Elo: " + oldElo + " | W:" + oldWins + " L:" + oldLosses + "\n" +
                        "Ahora:  Elo: 1000 | W:0 L:0\n" +
                        "```"
                    ).queue();
                } else {
                    event.getChannel().sendMessage("❌ El jugador no está en la base de datos").queue();
                }

            } catch (Exception e) {
                e.printStackTrace();
                event.getChannel().sendMessage("❌ Error al resetear el jugador").queue();
            }
        } else {
            event.getChannel().sendMessage("❌ Solo administradores pueden usar este comando").queue();
        }
    }
}
