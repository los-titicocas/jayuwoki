package dad.api.commands;

import dad.api.Commands;

public class RollaDie {

    private final int roll;

    public RollaDie(int sides) {

        // roll a die with the specified number of sides
        roll = (int) (Math.random() * sides) + 1;

    }

    @Override
    public String toString() {
        return "You rolled a " + roll;
    }
}
