package dad.database;

import dad.api.commands.Privadita;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Representa el estado de un servidor individual de Discord.
 * Cada servidor tiene su propia privadita activa, configuración de permisos, etc.
 * 
 * Esta clase permite que el bot funcione de forma independiente en múltiples servidores
 * sin que los comandos de un servidor afecten a otros.
 */
public class ServerState {
    
    private final String guildId;
    private final AtomicReference<Privadita> privadita;
    private final AtomicReference<Boolean> openPermissions;
    
    /**
     * Constructor del estado del servidor.
     * @param guildId ID único del servidor de Discord (Guild ID)
     */
    public ServerState(String guildId) {
        this.guildId = guildId;
        this.privadita = new AtomicReference<>();
        this.openPermissions = new AtomicReference<>(false);
    }
    
    // ==================== GETTERS ====================
    
    public String getGuildId() {
        return guildId;
    }
    
    public AtomicReference<Privadita> getPrivadita() {
        return privadita;
    }
    
    public AtomicReference<Boolean> getOpenPermissions() {
        return openPermissions;
    }
    
    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si hay una privadita activa en este servidor.
     * @return true si hay una privadita activa, false en caso contrario
     */
    public boolean hasActivePrivadita() {
        return privadita.get() != null;
    }
    
    /**
     * Elimina la privadita activa del servidor.
     */
    public void clearPrivadita() {
        privadita.set(null);
    }
    
    /**
     * Establece una nueva privadita activa en el servidor.
     * @param privadita La privadita a establecer
     */
    public void setPrivadita(Privadita privadita) {
        this.privadita.set(privadita);
    }
}
