package dad.database;


import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class pruebaDB {

    public static void main(String[] args) {
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
                    .setDatabaseUrl("https://<your-database-name>.firebaseio.com") // Reemplaza con tu URL
                    .build();

            FirebaseApp.initializeApp(options);

            // Obtener una instancia de Firestore
            Firestore db = FirestoreClient.getFirestore();

            System.out.println("Conexión a Firestore exitosa");

            // Ejemplo: Añadir datos a una colección
            DocumentReference docRef = db.collection("users").document("user2");
            ApiFuture<WriteResult> result = docRef.set(new User("pepe", 69));

            System.out.println("Datos escritos en Firestore en: " + result.get().getUpdateTime());
       } catch (IOException | InterruptedException | ExecutionException e) {
        e.printStackTrace();
        }
    }

    // Clase User actualizada
    static class User {
        private String name;
        private int age;

        // Constructor vacío (obligatorio para Firestore)
        public User() {
        }

        // Constructor con parámetros
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        // Getters y setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }


}
