package dad.api.commands;

import dad.api.Commands;

public class RollaDie {

    public int roll;

    public RollaDie(int sides) {
        roll = (int) (Math.random() * sides) + 1;
    }

    @Override
    public String toString() {
        return "You rolled a " + roll;
    }
}
