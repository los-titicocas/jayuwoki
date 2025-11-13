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
    
    // 🔐 SISTEMA DE PERMISOS JERÁRQUICO
    private static final List<Long> BOT_DEVELOPERS = new ArrayList<>();
    private static final List<Long> TRUSTED_USERS = new ArrayList<>();
    
    // Enum de niveles de permiso (de mayor a menor)
    public enum PermissionLevel {
        DEVELOPER(4, "Desarrollador del Bot"),
        TRUSTED(3, "Usuario de Confianza"),
        ADMIN(2, "Administrador del Servidor"),
        MODERATOR(1, "Moderador"),
        USER(0, "Usuario Normal");
        
        private final int level;
        private final String displayName;
        
        PermissionLevel(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
        
        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }
        
        // Comprueba si este nivel tiene suficientes permisos
        public boolean hasPermission(PermissionLevel required) {
            return this.level >= required.level;
        }
    }
    
    // Cargar IDs desde properties al iniciar
    static {
        loadPermissionLists();
    }
    
    private static void loadPermissionLists() {
        try {
            // Intentar cargar properties desde múltiples ubicaciones
            java.util.Properties props = new java.util.Properties();
            boolean loaded = false;
            
            // Intentar rutas posibles
            String[] possiblePaths = {
                "Jayuwoki/settings.properties",
                "settings.properties",
                "../settings.properties",
                System.getProperty("user.dir") + "/Jayuwoki/settings.properties",
                System.getProperty("user.dir") + "/settings.properties"
            };
            
            System.out.println("🔍 DEBUG: Buscando settings.properties...");
            System.out.println("🔍 DEBUG: Working directory: " + System.getProperty("user.dir"));
            
            for (String path : possiblePaths) {
                java.io.File file = new java.io.File(path);
                System.out.println("🔍 Probando: " + file.getAbsolutePath() + " → " + (file.exists() ? "✅" : "❌"));
                
                if (file.exists()) {
                    try (java.io.FileInputStream input = new java.io.FileInputStream(file)) {
                        props.load(input);
                        loaded = true;
                        System.out.println("✅ Archivo cargado desde: " + file.getAbsolutePath());
                        break;
                    }
                }
            }
            
            if (!loaded) {
                System.err.println("❌ No se pudo encontrar settings.properties en ninguna ubicación");
                return;
            }
            
            System.out.println("🔍 DEBUG: Properties cargadas:");
            System.out.println("  - Total properties: " + props.size());
            props.forEach((key, value) -> 
                System.out.println("    " + key + " = " + value));
            
            // Cargar desarrolladores
            String developers = props.getProperty("bot.developers", "");
            System.out.println("🔍 DEBUG: bot.developers = '" + developers + "'");
            if (!developers.isEmpty()) {
                Arrays.stream(developers.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .forEach(BOT_DEVELOPERS::add);
            }
            
            // Cargar usuarios de confianza
            String trusted = props.getProperty("trusted.users", "");
            System.out.println("🔍 DEBUG: trusted.users = '" + trusted + "'");
            if (!trusted.isEmpty()) {
                Arrays.stream(trusted.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .forEach(TRUSTED_USERS::add);
            }
            
            System.out.println("🔐 Permisos cargados:");
            System.out.println("  Desarrolladores: " + BOT_DEVELOPERS.size() + " → " + BOT_DEVELOPERS);
            System.out.println("  Usuarios de Confianza: " + TRUSTED_USERS.size() + " → " + TRUSTED_USERS);
        } catch (Exception e) {
            System.err.println("❌ ERROR al cargar permisos:");
            e.printStackTrace();
        }
    }

    // ==================== CONSTRUCTOR ====================
    
    public DBManager() {
        // 🔐 Cargar permisos si aún no se han cargado
        if (BOT_DEVELOPERS.isEmpty() && TRUSTED_USERS.isEmpty()) {
            System.out.println("🔄 Cargando permisos desde constructor...");
            loadPermissionLists();
        }
        
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
    /**
     * Añade un jugador a la base de datos (REQUIERE NIVEL MODERATOR).
     * @param newPlayer Nuevo jugador a añadir
     */
    public void AddPlayer(Player newPlayer) {
        try {
            String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
            ServerState state = getServerState(event);
            state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
            
            // 🔐 REQUIERE NIVEL MODERATOR (moderadores pueden añadir jugadores)
            if (CheckPermissions(state.getOpenPermissions().get(), PermissionLevel.MODERATOR)) {
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
     * Añade múltiples jugadores a la base de datos del servidor (REQUIERE NIVEL MODERATOR).
     * @param newPlayers Lista de jugadores a añadir
     */
    public void AddPlayers(List<Player> newPlayers) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        ServerState state = getServerState(event);
        state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        
        // 🔐 REQUIERE NIVEL MODERATOR
        if (CheckPermissions(state.getOpenPermissions().get(), PermissionLevel.MODERATOR)) {
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
    /**
     * Elimina un jugador de la base de datos (REQUIERE NIVEL ADMIN).
     * @param name Nombre del jugador a eliminar
     */
    public void DeletePlayer(String name) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        ServerState state = getServerState(event);
        state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        
        // 🔐 REQUIERE NIVEL ADMIN
        if (CheckPermissions(state.getOpenPermissions().get(), PermissionLevel.ADMIN)) {
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
                String seasonHeader = "📅 " + Utils.getSeasonHeader() + "\n\n";
                event.getChannel().sendMessage(seasonHeader + player.PrintStats()).queue();
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

            String seasonHeader = "📅 " + Utils.getSeasonHeader() + "\n\n";
            StringBuilder message = new StringBuilder(seasonHeader + "📊 **RANKING ELO - " + event.getGuild().getName() + "**\n\n```\n");
            message.append(String.format("%-3s %-20s %6s %4s %4s %7s\n", "#", "Jugador", "Elo", "W", "L", "WR"));
            message.append("─".repeat(50)).append("\n");

            int position = 1;
            for (DocumentSnapshot playerDoc : querySnapshot.getDocuments()) {
                Player player = playerDoc.toObject(Player.class);
                if (player != null) {
            message.append(String.format("%-3d %-20s %6d %4d %4d %6.2f%%\n",
                            position++,
                            player.getName(),
                            player.getElo(),
                            player.getWins(),
                            player.getLosses(),
                            player.getWinRate()));
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
     * Resetea manualmente el Elo de un jugador (REQUIERE NIVEL TRUSTED O SUPERIOR).
     * Este comando está protegido para evitar que admins del servidor lo usen sin control.
     * @param name Nombre del jugador a resetear
     */
    public void AdminResetPlayerElo(String name) {
        String guildId = event.getGuild().getId(); // ✨ Usar Guild ID
        ServerState state = getServerState(event);
        state.getOpenPermissions().set(Boolean.parseBoolean(Utils.properties.getProperty("massPermissionCheck")));
        
        // 🔐 REQUIERE NIVEL TRUSTED: Solo desarrolladores y usuarios de confianza
        if (CheckPermissions(state.getOpenPermissions().get(), PermissionLevel.TRUSTED)) {
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
     * Obtiene el nivel de permiso del usuario actual.
     * Jerarquía: DEVELOPER > TRUSTED > ADMIN > MODERATOR > USER
     * @param member Usuario de Discord
     * @return Nivel de permiso del usuario
     */
    private PermissionLevel getUserPermissionLevel(Member member) {
        if (member == null) return PermissionLevel.USER;
        
        long userId = member.getIdLong();
        System.out.println("🔍 DEBUG: Checking permissions for user ID: " + userId);
        System.out.println("🔍 DEBUG: BOT_DEVELOPERS list: " + BOT_DEVELOPERS);
        System.out.println("🔍 DEBUG: TRUSTED_USERS list: " + TRUSTED_USERS);
        
        // 1. Comprobar si es desarrollador del bot (máximo nivel)
        if (BOT_DEVELOPERS.contains(userId)) {
            System.out.println("✅ DEBUG: User is DEVELOPER");
            return PermissionLevel.DEVELOPER;
        }
        
        // 2. Comprobar si es usuario de confianza
        if (TRUSTED_USERS.contains(userId)) {
            System.out.println("✅ DEBUG: User is TRUSTED");
            return PermissionLevel.TRUSTED;
        }
        
        // 3. Comprobar permisos de Discord en el servidor
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            System.out.println("✅ DEBUG: User is ADMIN");
            return PermissionLevel.ADMIN;
        }
        
        if (member.hasPermission(Permission.MANAGE_SERVER) || 
            member.hasPermission(Permission.KICK_MEMBERS) ||
            member.hasPermission(Permission.BAN_MEMBERS)) {
            System.out.println("✅ DEBUG: User is MODERATOR");
            return PermissionLevel.MODERATOR;
        }
        
        // 4. Usuario normal
        System.out.println("❌ DEBUG: User is USER (normal)");
        return PermissionLevel.USER;
    }

    /**
     * Verifica si el usuario tiene permisos suficientes para ejecutar una acción.
     * Sistema jerárquico: Developers pueden todo, Trusted pueden comandos críticos,
     * Admins comandos normales, etc.
     * @param openPermissions Si true, todos tienen permisos (modo abierto)
     * @param requiredLevel Nivel mínimo requerido para la acción
     * @return true si el usuario tiene permisos suficientes
     */
    private boolean CheckPermissions(Boolean openPermissions, PermissionLevel requiredLevel) {
        // Modo abierto: todos pueden
        if (openPermissions) {
            return true;
        }
        
        PermissionLevel userLevel = getUserPermissionLevel(discordUser);
        
        // Comprobar si tiene suficiente nivel
        if (userLevel.hasPermission(requiredLevel)) {
            return true;
        } else {
            event.getChannel().sendMessage(String.format(
                "❌ **No tienes permisos para esta acción.**\n" +
                "Tu nivel: **%s** | Se requiere: **%s** o superior",
                userLevel.getDisplayName(),
                requiredLevel.getDisplayName()
            )).queue();
            return false;
        }
    }
    
    /**
     * Sobrecarga del método CheckPermissions para mantener compatibilidad.
     * Por defecto requiere nivel ADMIN.
     */
    private boolean CheckPermissions(Boolean openPermissions) {
        return CheckPermissions(openPermissions, PermissionLevel.ADMIN);
    }
    
    /**
     * Método público para recargar las listas de permisos desde properties.
     * Útil para actualizar permisos sin reiniciar el bot.
     */
    public static void reloadPermissions() {
        BOT_DEVELOPERS.clear();
        TRUSTED_USERS.clear();
        loadPermissionLists();
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
