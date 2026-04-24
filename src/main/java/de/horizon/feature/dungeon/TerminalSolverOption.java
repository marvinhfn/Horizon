package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;

public enum TerminalSolverOption {
    CORRECT_ALL("Correct All The Panes", "Markiert die gueltigen Panes im Terminal."),
    NAVIGATE_MAZE("Navigate The Maze", "Zeigt den gueltigen Weg durch das Maze an."),
    CLICK_IN_ORDER("Click In Order", "Hebt die Reihenfolge der Zahlen hervor."),
    STARTS_WITH("Starts With", "Filtert Items nach dem geforderten Anfangsbuchstaben."),
    SELECT_ALL_COLOR("Select All Color", "Markiert alle Items der benoetigten Farbe."),
    SAME_COLOR("Change All To Same Color", "Hilft beim Angleichen auf eine Farbe.");

    private final String title;
    private final String description;

    TerminalSolverOption(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public boolean isEnabled(HorizonConfig config) {
        return switch (this) {
            case CORRECT_ALL -> config.isTerminalCorrectAllEnabled();
            case NAVIGATE_MAZE -> config.isTerminalNavigateMazeEnabled();
            case CLICK_IN_ORDER -> config.isTerminalClickInOrderEnabled();
            case STARTS_WITH -> config.isTerminalStartsWithEnabled();
            case SELECT_ALL_COLOR -> config.isTerminalSelectAllColorEnabled();
            case SAME_COLOR -> config.isTerminalSameColorEnabled();
        };
    }

    public void toggle(HorizonConfig config) {
        switch (this) {
            case CORRECT_ALL -> config.setTerminalCorrectAllEnabled(!config.isTerminalCorrectAllEnabled());
            case NAVIGATE_MAZE -> config.setTerminalNavigateMazeEnabled(!config.isTerminalNavigateMazeEnabled());
            case CLICK_IN_ORDER -> config.setTerminalClickInOrderEnabled(!config.isTerminalClickInOrderEnabled());
            case STARTS_WITH -> config.setTerminalStartsWithEnabled(!config.isTerminalStartsWithEnabled());
            case SELECT_ALL_COLOR -> config.setTerminalSelectAllColorEnabled(!config.isTerminalSelectAllColorEnabled());
            case SAME_COLOR -> config.setTerminalSameColorEnabled(!config.isTerminalSameColorEnabled());
        }
    }
}
