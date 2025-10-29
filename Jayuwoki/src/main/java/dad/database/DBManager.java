package dad.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import dad.api.commands.Privadita;
import dad.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DBManager {

    // ==================== ATRIBUTOS ====================
    
    private Firestore db;
    private MessageReceivedEvent event;
    private Member discordUser;
    
    // ✨ NUEVO: Map de estados por servidor (Thread-safe)
    private final Map<String, ServerState> serverStates = new ConcurrentHashMap<>();
    
    // ❌ ELIMINADO: currentServer y openPermissions ya no son globales
    // Ahora cada servidor tiene su propio estado en ServerState

    // ==================== CONSTRUCTOR ====================
    
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
                    .setDatabaseUrl("https://JayuwokiDB.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);

            // Obtener una instancia de Firestore
            db = FirestoreClient.getFirestore();
            
            System.out.println("✅ Firebase inicializado correctamente");
            System.out.println("✅ Sistema multi-servidor activo");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== GESTIÓN DE ESTADO POR SERVIDOR ====================
    
    /**
     * Obtiene o crea el estado para un servidor específico.
     * @param guildId ID del servidor de Discord
     * @return El estado del servidor
     */
    private ServerState getServerState(String guildId) {
        return serverStates.computeIfAbsent(guildId, ServerState::new);
    }

    /**
     * Obtiene el estado del servidor desde el evento de Discord.
     * @param event Evento de Discord
     * @return El estado del servidor
     */
    private ServerState getServerState(MessageReceivedEvent event) {
        return getServerState(event.getGuild().getId());
    }

    /**
     * Verifica si hay una privadita activa en el servidor del evento.
     * @param event Evento de Discord
     * @return true si hay privadita activa
     */
    public boolean hasActivePrivadita(MessageReceivedEvent event) {
        return getServerState(event).hasActivePrivadita();
    }

    /**
     * Obtiene la privadita activa del servidor.
     * @param event Evento de Discord
     * @return La privadita activa, o null si no hay ninguna
     */
    public Privadita getActivePrivadita(MessageReceivedEvent event) {
        return getServerState(event).getPrivadita().get();
    }

    /**
     * Establece una privadita activa en el servidor.
     * @param event Evento de Discord
     * @param privadita La privadita a establecer
     */
    public void setActivePrivadita(MessageReceivedEvent event, Privadita privadita) {
        getServerState(event).setPrivadita(privadita);
        System.out.println("✅ Privadita creada en servidor: " + event.getGuild().getName() + 
                          " (" + event.getGuild().getId() + ")");
    }

    /**
     * Elimina la privadita activa del servidor.
     * @param event Evento de Discord
     */
    public void clearActivePrivadita(MessageReceivedEvent event) {
        getServerState(event).clearPrivadita();
        System.out.println("🗑️ Privadita eliminada en servidor: " + event.getGuild().getName() + 
                          " (" + event.getGuild().getId() + ")");
    }

    // ==================== MÉTODOS DE JUGADORES ====================

    /**
     * Obtiene jugadores desde Firebase por sus nombres.
     * Ahora usa el Guild ID del servidor para acceder a la colección correcta.
     * 
     * @param nombres Array con los nombres de los jugadores a buscar
     * @param event Evento de Discord para obtener el servidor y enviar mensajes
     * @return Lista de jugadores encontrados con sus datos actualizados
     */
    public List<Player> GetPlayers(String[] nombres, MessageReceivedEvent event) {
        List<Player> players = new ArrayList<>();
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID del servidor
        
        CollectionReference playersCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

        try {
            QuerySnapshot querySnapshot = playersCollection
                    .whereIn("name", Arrays.asList(nombres))
                    .get()
                    .get();

            WriteBatch batch = db.batch();
            boolean hasDefaultEloUpdates = false;

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Player player = doc.toObject(Player.class);
                if (player != null) {
                    int totalGames = player.getWins() + player.getLosses();
                    boolean hasAnomalousElo = false;
                    
                    if (player.getElo() < 800 && totalGames == 0) {
                        hasAnomalousElo = true;
                        System.out.println("⚠️ BUG DETECTADO en " + event.getGuild().getName() + 
                                         ": Jugador '" + player.getName() + 
                                         "' tiene Elo = " + player.getElo() + " pero 0 partidas. " +
                                         "Reiniciando a 1000...");
                    } else if (player.getElo() < 800 && totalGames > 0 && totalGames <= 3) {
                        hasAnomalousElo = true;
                        System.out.println("⚠️ BUG DETECTADO en " + event.getGuild().getName() + 
                                         ": Jugador '" + player.getName() + 
                                         "' tiene Elo = " + player.getElo() + 
                                         " con solo " + totalGames + " partidas. " +
                                         "Esto es imposible matemáticamente. Reiniciando a 1000...");
                    } else if (player.getElo() < 800 && totalGames > 3) {
                        System.out.println("ℹ️ INFO en " + event.getGuild().getName() + 
                                         ": Jugador '" + player.getName() + 
                                         "' tiene Elo bajo (" + player.getElo() + 
                                         ") pero es legítimo (W:" + player.getWins() + 
                                         " L:" + player.getLosses() + ")");
                    }
                    
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
                    System.out.println("✅ Bugs de Elo corregidos en servidor: " + event.getGuild().getName());
                } catch (Exception be) {
                    System.out.println("⚠️ Error al guardar correcciones en servidor: " + event.getGuild().getName());
                    be.printStackTrace();
                }
            }

            List<String> jugadoresEncontrados = players.stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());

            List<String> jugadoresNoEncontrados = Arrays.stream(nombres)
                    .filter(nombre -> !jugadoresEncontrados.contains(nombre))
                    .collect(Collectors.toList());

            if (!jugadoresNoEncontrados.isEmpty()) {
                StringBuilder message = new StringBuilder("❌ **Los siguientes jugadores no están en la base de datos de este servidor:**\n");
                for (String nombre : jugadoresNoEncontrados) {
                    message.append("- `").append(nombre).append("`\n");
                }
                message.append("\n💡 Usa `$addPlayer <nombre>` para añadirlos.");
                event.getChannel().sendMessage(message.toString().trim()).queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return players;
    }

    /**
     * Actualiza la lista de jugadores en Firebase.
     * Ahora usa el Guild ID del servidor.
     * 
     * @param event Evento de Discord para obtener el servidor
     * @param players Lista de jugadores con datos actualizados
     */
    public void updatePlayers(MessageReceivedEvent event, javafx.collections.ObservableList<Player> players) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        System.out.println("Actualizando jugadores en servidor: " + event.getGuild().getName() + " (" + guildId + ")");

        CollectionReference playersCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

        WriteBatch batch = db.batch();

        for (Player player : players) {
            System.out.println("  Actualizando: " + player.getName() + 
                             " - Elo: " + player.getElo() + 
                             " - W:" + player.getWins() + 
                             " L:" + player.getLosses());

            DocumentReference playerDoc = playersCollection.document(player.getName());
            batch.set(playerDoc, player);
        }

        try {
            batch.commit().get();
            System.out.println("✅ Jugadores actualizados en servidor: " + event.getGuild().getName());
        } catch (Exception e) {
            System.out.println("❌ Error al actualizar jugadores en servidor: " + event.getGuild().getName());
            e.printStackTrace();
        }
    }

    /**
     * Añade un jugador a la base de datos del servidor.
     * @param newPlayer Jugador a añadir
     */
    public void AddPlayer(Player newPlayer) {
        try {
            String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
            ServerState state = getServerState(event);
            state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
            
            if (CheckPermissions(state.getOpenPermissions().get())) {
                if (!CheckPlayerFound(newPlayer)) {
                    event.getChannel().sendMessage("❌ **El jugador ya está en la base de datos de este servidor.**").queue();
                    return;
                }
                
                CollectionReference playersCollection = db.collection(guildId)
                        .document("Privadita")
                        .collection("Players");

                playersCollection.document(newPlayer.getName()).set(newPlayer);
                event.getChannel().sendMessage("✅ **Jugador `" + newPlayer.getName() + "` añadido a la base de datos.**").queue();
                
                System.out.println("✅ Jugador '" + newPlayer.getName() + "' añadido en servidor: " + 
                                 event.getGuild().getName() + " (" + guildId + ")");
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Añade múltiples jugadores a la base de datos del servidor.
     * @param newPlayers Lista de jugadores a añadir
     */
    public void AddPlayers(List<Player> newPlayers) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        ServerState state = getServerState(event);
        state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        
        if (CheckPermissions(state.getOpenPermissions().get())) {
            try {
                List<Player> playersNotInDB = GetPlayersNotFound(newPlayers);
                List<Player> playersAlreadyInDB = newPlayers.stream()
                        .filter(player -> !playersNotInDB.contains(player))
                        .collect(Collectors.toList());

                if (!playersAlreadyInDB.isEmpty()) {
                    StringBuilder message = new StringBuilder("ℹ️ **Los siguientes jugadores ya están en la base de datos:**\n");
                    for (Player player : playersAlreadyInDB) {
                        message.append("- `").append(player.getName()).append("`\n");
                    }
                    event.getChannel().sendMessage(message.toString().trim()).queue();
                }

                if (playersNotInDB.isEmpty()) {
                    event.getChannel().sendMessage("❌ **Todos los jugadores ya están en la base de datos. No se añaden nuevos jugadores.**").queue();
                    return;
                }

                StringBuilder addMessage = new StringBuilder("✅ **Los siguientes jugadores se añadieron a la base de datos:**\n");
                CollectionReference playersCollection = db.collection(guildId)
                        .document("Privadita")
                        .collection("Players");
                        
                WriteBatch batch = db.batch();

                for (Player player : playersNotInDB) {
                    DocumentReference playerDoc = playersCollection.document(player.getName());
                    batch.set(playerDoc, player);
                    addMessage.append("- `").append(player.getName()).append("`\n");
                }

                batch.commit().get();
                event.getChannel().sendMessage(addMessage.toString().trim()).queue();
                
                System.out.println("✅ " + playersNotInDB.size() + " jugadores añadidos en servidor: " + 
                                 event.getGuild().getName() + " (" + guildId + ")");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Elimina un jugador de la base de datos del servidor.
     * @param name Nombre del jugador a eliminar
     */
    public void DeletePlayer(String name) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        ServerState state = getServerState(event);
        state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        
        if (CheckPermissions(state.getOpenPermissions().get())) {
            CollectionReference playersCollection = db.collection(guildId)
                    .document("Privadita")
                    .collection("Players");

            try {
                DocumentSnapshot playerDoc = playersCollection.document(name).get().get();

                if (playerDoc.exists()) {
                    playersCollection.document(name).delete();
                    event.getChannel().sendMessage("✅ **El jugador `" + name + "` ha sido eliminado de la base de datos.**").queue();
                    
                    System.out.println("🗑️ Jugador '" + name + "' eliminado en servidor: " + 
                                     event.getGuild().getName() + " (" + guildId + ")");
                } else {
                    event.getChannel().sendMessage("❌ **El jugador no está en la base de datos de este servidor.**").queue();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Muestra las estadísticas de un jugador específico.
     * @param name Nombre del jugador
     */
    public void ShowPlayerElo(String name) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        CollectionReference playersCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

        try {
            DocumentSnapshot playerDoc = playersCollection.document(name).get().get();

            if (playerDoc.exists()) {
                Player player = playerDoc.toObject(Player.class);
                event.getChannel().sendMessage(player.PrintStats()).queue();
            } else {
                event.getChannel().sendMessage("❌ **El jugador `" + name + "` no está en la base de datos de este servidor.**").queue();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Muestra el ranking de todos los jugadores del servidor ordenados por Elo.
     */
    public void ShowAllElo() {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        CollectionReference playersCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

        try {
            QuerySnapshot querySnapshot = playersCollection
                    .orderBy("elo", Query.Direction.DESCENDING)
                    .get()
                    .get();

            if (querySnapshot.isEmpty()) {
                event.getChannel().sendMessage("❌ **No hay jugadores registrados en este servidor.**").queue();
                return;
            }

            StringBuilder message = new StringBuilder("📊 **RANKING ELO - " + event.getGuild().getName() + "**\n\n```\n");
            message.append(String.format("%-3s %-20s %6s %4s %4s\n", "#", "Jugador", "Elo", "W", "L"));
            message.append("─".repeat(42)).append("\n");

            int position = 1;
            for (DocumentSnapshot playerDoc : querySnapshot.getDocuments()) {
                Player player = playerDoc.toObject(Player.class);
                if (player != null) {
                    message.append(String.format("%-3d %-20s %6d %4d %4d\n",
                            position++,
                            player.getName(),
                            player.getElo(),
                            player.getWins(),
                            player.getLosses()));
                }
            }

            message.append("```");
            event.getChannel().sendMessage(message.toString()).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Verifica si un jugador existe en la base de datos del servidor.
     * @param player Jugador a verificar
     * @return true si el jugador NO existe (disponible para añadir)
     */
    public boolean CheckPlayerFound(Player player) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        CollectionReference playersCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

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

    /**
     * Obtiene la lista de jugadores que NO están en la base de datos del servidor.
     * @param players Lista de jugadores a verificar
     * @return Lista de jugadores no encontrados
     */
    public List<Player> GetPlayersNotFound(List<Player> players) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        CollectionReference playersCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

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

    /**
     * Resetea manualmente el Elo de un jugador (solo administradores).
     * @param name Nombre del jugador a resetear
     */
    public void AdminResetPlayerElo(String name) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        ServerState state = getServerState(event);
        state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        
        if (CheckPermissions(state.getOpenPermissions().get())) {
            CollectionReference playersCollection = db.collection(guildId)
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
                    
                    System.out.println("🔧 Admin reset en " + event.getGuild().getName() + 
                                     ": " + name + " (Elo: " + oldElo + " → 1000)");
                } else {
                    event.getChannel().sendMessage("❌ **El jugador no está en la base de datos de este servidor.**").queue();
                }

            } catch (Exception e) {
                e.printStackTrace();
                event.getChannel().sendMessage("❌ **Error al resetear el jugador.**").queue();
            }
        } else {
            event.getChannel().sendMessage("❌ **Solo administradores pueden usar este comando.**").queue();
        }
    }

    // ==================== MÉTODOS DE PERMISOS ====================

    /**
     * Verifica si el usuario tiene permisos para modificar la base de datos.
     * @param openPermissions Si true, todos tienen permisos. Si false, solo admins.
     * @return true si el usuario tiene permisos
     */
    private boolean CheckPermissions(Boolean openPermissions) {
        if (openPermissions) {
            return true;
        } else if (discordUser.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        } else {
            event.getChannel().sendMessage("❌ **No tienes permisos para modificar la base de datos.**").queue();
            return false;
        }
    }

    // ==================== GETTERS Y SETTERS ====================

    public Firestore getDb() {
        return db;
    }

    public void setEvent(MessageReceivedEvent event) {
        this.event = event;
        this.discordUser = event.getMember();
    }
    
    /**
     * Obtiene el Guild ID del servidor actual del evento.
     * @return Guild ID del servidor
     */
    public String getCurrentServer() {
        return event != null ? event.getGuild().getId() : null;
    }
}
