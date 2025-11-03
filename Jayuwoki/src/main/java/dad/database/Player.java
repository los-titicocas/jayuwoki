package dad.database;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Player {

    // Attributes
    private final IntegerProperty idPlayer = new SimpleIntegerProperty();
    private final StringProperty discriminator = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty avatarURL = new SimpleStringProperty();
    private final IntegerProperty wins = new SimpleIntegerProperty();
    private final IntegerProperty losses = new SimpleIntegerProperty();
    private final IntegerProperty elo = new SimpleIntegerProperty();

    private final StringProperty role = new SimpleStringProperty();

    public Player() {
    }

    public Player(int idPlayer, String discriminator, String name, String avatarURL, int wins, int losses, int elo) {
        this.idPlayer.set(idPlayer);
        this.discriminator.set(discriminator);
        this.name.set(name);
        this.avatarURL.set(avatarURL);
        this.wins.set(wins);
        this.losses.set(losses);
        this.elo.set(elo);
    }

    public Player(String name, int elo) {
        this.name.set(name);
        this.elo.set(elo);
    }

    /**
     * Calcula el Win Rate (porcentaje de victorias) del jugador.
     * @return Win Rate como porcentaje (0.0 - 100.0)
     */
    public double getWinRate() {
        int totalGames = wins.get() + losses.get();
        if (totalGames == 0) {
            return 0.0;
        }
        return (wins.get() * 100.0) / totalGames;
    }
    
    /**
     * Obtiene el Win Rate formateado como String sin decimales.
     * @return Win Rate formateado
     */
    public String getWinRateFormatted() {
        return String.format("%.0f%%", getWinRate());
    }

    public String PrintStats() {
        StringBuilder stats = new StringBuilder();

        stats.append("Player: ").append(name.get()).append("#").append(discriminator.get()).append("\n");
        stats.append("Wins: ").append(wins.get()).append("\n");
        stats.append("Losses: ").append(losses.get()).append("\n");
        stats.append("Elo: ").append(elo.get()).append("\n");
        stats.append("Win Rate: ").append(getWinRateFormatted()).append("\n");

        return stats.toString(); // Retorna el mensaje como un String
    }


    /**
     * Actualiza el Elo del jugador basándose en el resultado de la partida.
     * Utiliza el sistema de clasificación Elo similar al de League of Legends.
     * 
     * La fórmula calcula:
     * 1. Probabilidad esperada de victoria: E = 1 / (1 + 10^((EloEnemigo - EloJugador) / 400))
     * 2. Cambio de Elo: ΔElo = K × (Resultado - Probabilidad Esperada)
     * 
     * El factor K determina la volatilidad del cambio:
     * - K alto (40) = Cambios más drásticos, ideal para jugadores nuevos
     * - K medio (32) = Balance entre estabilidad y movilidad
     * - K bajo (16-24) = Cambios graduales, para jugadores experimentados
     * 
     * @param averageEnemyElo El Elo promedio del equipo enemigo
     * @param won true si el jugador ganó la partida, false si perdió
     */
    public void ActualizarElo(double averageEnemyElo, boolean won) {
        // Constante K - En LoL varía según el rango y partidas jugadas
        // Usamos K=32 como valor estándar (balance entre cambios significativos y estabilidad)
        final int K = 32;
        
        // Calcular la probabilidad esperada de victoria usando la fórmula de Elo
        // E = 1 / (1 + 10^((R_enemigo - R_jugador) / 400))
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (averageEnemyElo - this.elo.get()) / 400.0));
        
        // El resultado real: 1 si ganó, 0 si perdió
        double actualScore = won ? 1.0 : 0.0;
        
        // Calcular el cambio de Elo
        // ΔElo = K × (S - E) donde S es el resultado real y E es el esperado
        int eloChange = (int) Math.round(K * (actualScore - expectedScore));
        
        // Aplicar el cambio al Elo actual
        int newElo = this.elo.get() + eloChange;
        
        // Aplicar límites: El Elo mínimo es 0 (no puede ser negativo)
        if (newElo < 0) {
            newElo = 0;
        }
        
        this.elo.set(newElo);
    }

    public int getIdPlayer() {
        return idPlayer.get();
    }

    public IntegerProperty idPlayerProperty() {
        return idPlayer;
    }

    public void setIdPlayer(int idPlayer) {
        this.idPlayer.set(idPlayer);
    }

    public String getDiscriminator() {
        return discriminator.get();
    }

    public StringProperty discriminatorProperty() {
        return discriminator;
    }

    public void setDiscriminator(String discriminator) {
        this.discriminator.set(discriminator);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getAvatarURL() {
        return avatarURL.get();
    }

    public StringProperty avatarURLProperty() {
        return avatarURL;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL.set(avatarURL);
    }

    public int getWins() {
        return wins.get();
    }

    public IntegerProperty winsProperty() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins.set(wins);
    }

    public int getLosses() {
        return losses.get();
    }

    public IntegerProperty lossesProperty() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses.set(losses);
    }

    public int getElo() {
        return elo.get();
    }

    public IntegerProperty eloProperty() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo.set(elo);
    }

    public String getRole() {
        return role.get();
    }

    public StringProperty roleProperty() {
        return role;
    }

    public void setRole(String role) {
        this.role.set(role);
    }

}
