package dad.api.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.dv8tion.jda.api.entities.User;

public class Player  {
    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();

    public Player() {
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

    public String getPlayerName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }


}
