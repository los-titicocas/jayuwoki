package dad.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import dad.database.Player;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Script de migración de datos de la estructura antigua (global) 
 * a la nueva estructura (por servidor con Guild ID).
 * 
 * ESTRUCTURA ANTIGUA:
 * jayuwokidb/
 * └── Servers/
 *     └── Privadita/  ← Documento
 *         └── Players/  ← Colección
 *             ├── Jorge
 *             ├── Messi
 *             └── ...
 * 
 * ESTRUCTURA NUEVA:
 * jayuwokidb/
 * ├── 123456789012345678/  ← Guild ID del Server A (Colección)
 * │   └── Privadita/  ← Documento
 * │       └── Players/  ← Colección
 * │           ├── Jorge
 * │           └── Messi
 * ├── 987654321098765432/  ← Guild ID del Server B (Colección)
 * │   └── Privadita/  ← Documento
 * │       └── Players/  ← Colección
 * │           ├── Chris
 * │           └── Nuriel
 * └── _backup_old_structure/  ← Backup de datos antiguos (Colección)
 *     └── backup/  ← Documento
 *         └── Players/  ← Colección
 *             └── ...
 */
public class MigrationScript {

    private final Firestore db;
    private final Scanner scanner;

    public MigrationScript() {
        // Inicializar Firebase si no está inicializado
        initializeFirebase();
        
        this.db = FirestoreClient.getFirestore();
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * Inicializa Firebase con las credenciales del archivo jayuwokidb-firebase-adminsdk.json
     */
    private void initializeFirebase() {
        try {
            // Verificar si Firebase ya está inicializado
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(
                        "src/main/resources/jayuwokidb-firebase-adminsdk.json");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase inicializado correctamente\n");
            } else {
                System.out.println("✅ Firebase ya estaba inicializado\n");
            }
        } catch (IOException e) {
            System.err.println("❌ ERROR: No se pudo inicializar Firebase");
            System.err.println("   Asegúrate de que existe el archivo:");
            System.err.println("   src/main/resources/jayuwokidb-firebase-adminsdk.json");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     JAYUWOKI BOT - SCRIPT DE MIGRACIÓN DE DATOS          ║");
        System.out.println("║     De estructura global a estructura por servidor        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        MigrationScript migration = new MigrationScript();
        migration.run();
    }

    public void run() {
        try {
            System.out.println("🔍 PASO 1: Verificando datos existentes...\n");

            // Verificar si hay datos antiguos
            List<Player> oldPlayers = getOldPlayers();

            if (oldPlayers.isEmpty()) {
                System.out.println("✅ No se encontraron datos en la estructura antigua.");
                System.out.println("   Tu base de datos ya está usando la nueva estructura o está vacía.");
                System.out.println("   No es necesario migrar.\n");
                return;
            }

            System.out.println("📊 Se encontraron " + oldPlayers.size() + " jugadores en la estructura antigua:\n");
            
            // Mostrar primeros 10 jugadores
            int count = 0;
            for (Player player : oldPlayers) {
                if (count++ >= 10) {
                    System.out.println("   ... y " + (oldPlayers.size() - 10) + " más");
                    break;
                }
                System.out.printf("   - %-20s | Elo: %4d | W:%3d L:%3d\n",
                        player.getName(),
                        player.getElo(),
                        player.getWins(),
                        player.getLosses());
            }

            System.out.println("\n⚠️  ADVERTENCIA: Esta migración copiará estos datos a uno o más servidores.\n");
            System.out.println("¿Deseas continuar? (sí/no): ");
            String response = scanner.nextLine().trim().toLowerCase();

            if (!response.equals("sí") && !response.equals("si") && !response.equals("yes") && !response.equals("s")) {
                System.out.println("\n❌ Migración cancelada.");
                return;
            }

            System.out.println("\n🔍 PASO 2: Configurando servidores de destino...\n");

            // Obtener IDs de servidores
            List<String> guildIds = getGuildIds();

            if (guildIds.isEmpty()) {
                System.out.println("❌ No se especificaron servidores. Migración cancelada.");
                return;
            }

            System.out.println("\n💾 PASO 3: Creando backup de datos antiguos...\n");

            // Crear backup
            backupOldData(oldPlayers);

            System.out.println("\n📦 PASO 4: Migrando datos a nueva estructura...\n");

            // Migrar datos
            for (String guildId : guildIds) {
                migrateToGuild(guildId, oldPlayers);
            }

            System.out.println("\n✅ PASO 5: Verificando migración...\n");

            // Verificar migración
            boolean verified = verifyMigration(guildIds, oldPlayers);

            if (verified) {
                System.out.println("✅ Verificación exitosa. Todos los datos se migraron correctamente.\n");
                
                System.out.println("🗑️  ¿Deseas eliminar los datos antiguos? (sí/no): ");
                response = scanner.nextLine().trim().toLowerCase();

                if (response.equals("sí") || response.equals("si") || response.equals("yes") || response.equals("s")) {
                    deleteOldData();
                    System.out.println("✅ Datos antiguos eliminados. Migración completada.\n");
                } else {
                    System.out.println("ℹ️  Los datos antiguos se mantuvieron. Puedes eliminarlos manualmente más tarde.\n");
                }
            } else {
                System.out.println("⚠️  Hubo problemas en la verificación. Revisa manualmente la base de datos.\n");
                System.out.println("   Los datos antiguos NO se eliminaron por seguridad.");
            }

            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║                  MIGRACIÓN COMPLETADA                      ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("\n❌ ERROR durante la migración:");
            e.printStackTrace();
            System.out.println("\n💡 Tus datos antiguos están intactos en la estructura original.");
        }
    }

    /**
     * Lee todos los jugadores de la estructura antigua.
     */
    private List<Player> getOldPlayers() throws ExecutionException, InterruptedException {
        List<Player> players = new ArrayList<>();

        // Estructura antigua: Servers > "Privadita" (documento) > "Players" (colección)
        CollectionReference oldCollection = db.collection("Servers")
                .document("Privadita")
                .collection("Players");
        
        QuerySnapshot querySnapshot = oldCollection.get().get();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            Player player = doc.toObject(Player.class);
            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    /**
     * Solicita los IDs de los servidores de Discord.
     */
    private List<String> getGuildIds() {
        List<String> guildIds = new ArrayList<>();

        System.out.println("Introduce los Guild IDs de los servidores (uno por línea).");
        System.out.println("Para obtener el Guild ID:");
        System.out.println("  1. Activa el 'Modo Desarrollador' en Discord");
        System.out.println("  2. Clic derecho en el servidor → Copiar ID");
        System.out.println("\nIntroduce 'fin' cuando termines:\n");

        while (true) {
            System.out.print("Guild ID: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("fin") || input.equalsIgnoreCase("done")) {
                break;
            }

            if (input.matches("\\d{17,19}")) { // Discord IDs tienen 17-19 dígitos
                guildIds.add(input);
                System.out.println("✅ Guild ID añadido: " + input);
            } else {
                System.out.println("❌ ID inválido. Debe ser un número de 17-19 dígitos.");
            }
        }

        return guildIds;
    }

    /**
     * Crea un backup de los datos antiguos.
     */
    private void backupOldData(List<Player> players) throws ExecutionException, InterruptedException {
        // Backup: _backup_old_structure (colección) > backup (documento) > Players (colección)
        CollectionReference backupCollection = db.collection("_backup_old_structure")
                .document("backup")
                .collection("Players");

        WriteBatch batch = db.batch();
        int count = 0;

        for (Player player : players) {
            DocumentReference docRef = backupCollection.document(player.getName());
            batch.set(docRef, player);
            count++;

            // Firebase limita a 500 operaciones por batch
            if (count % 500 == 0) {
                batch.commit().get();
                batch = db.batch();
                System.out.println("   💾 Backup: " + count + " jugadores guardados...");
            }
        }

        // Commit de las operaciones restantes
        if (count % 500 != 0) {
            batch.commit().get();
        }

        System.out.println("✅ Backup completado: " + count + " jugadores respaldados en '_backup_old_structure'");
    }

    /**
     * Migra los datos a un servidor específico.
     */
    private void migrateToGuild(String guildId, List<Player> players) throws ExecutionException, InterruptedException {
        CollectionReference newCollection = db.collection(guildId)
                .document("Privadita")
                .collection("Players");

        WriteBatch batch = db.batch();
        int count = 0;

        System.out.println("📦 Migrando a servidor: " + guildId);

        for (Player player : players) {
            DocumentReference docRef = newCollection.document(player.getName());
            batch.set(docRef, player);
            count++;

            if (count % 500 == 0) {
                batch.commit().get();
                batch = db.batch();
                System.out.println("   ⏳ Migrados: " + count + "/" + players.size() + " jugadores...");
            }
        }

        if (count % 500 != 0) {
            batch.commit().get();
        }

        System.out.println("✅ Servidor " + guildId + ": " + count + " jugadores migrados");
    }

    /**
     * Verifica que la migración se completó correctamente.
     */
    private boolean verifyMigration(List<String> guildIds, List<Player> originalPlayers) throws ExecutionException, InterruptedException {
        boolean allVerified = true;

        for (String guildId : guildIds) {
            CollectionReference newCollection = db.collection(guildId)
                    .document("Privadita")
                    .collection("Players");

            QuerySnapshot querySnapshot = newCollection.get().get();
            int migratedCount = querySnapshot.size();

            System.out.printf("   Server %s: %d/%d jugadores ✓\n", 
                    guildId, migratedCount, originalPlayers.size());

            if (migratedCount != originalPlayers.size()) {
                System.out.println("   ⚠️  ADVERTENCIA: El número de jugadores no coincide");
                allVerified = false;
            }
        }

        return allVerified;
    }

    /**
     * Elimina los datos antiguos después de verificar la migración.
     */
    private void deleteOldData() throws ExecutionException, InterruptedException {
        // Estructura antigua: Servers > "Privadita" (documento) > "Players" (colección)
        CollectionReference oldCollection = db.collection("Servers")
                .document("Privadita")
                .collection("Players");
        
        QuerySnapshot querySnapshot = oldCollection.get().get();

        WriteBatch batch = db.batch();
        int count = 0;

        System.out.println("🗑️  Eliminando datos antiguos...");

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            batch.delete(doc.getReference());
            count++;

            if (count % 500 == 0) {
                batch.commit().get();
                batch = db.batch();
                System.out.println("   🗑️  Eliminados: " + count + " documentos...");
            }
        }

        if (count % 500 != 0) {
            batch.commit().get();
        }

        System.out.println("✅ Eliminados " + count + " documentos de la estructura antigua");
    }
}
