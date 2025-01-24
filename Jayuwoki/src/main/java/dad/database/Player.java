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
