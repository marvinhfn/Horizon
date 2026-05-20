package de.horizon.feature.dungeon;

import de.horizon.Lang;
import de.horizon.config.HorizonConfig;

public enum TerminalSolverOption {
    CORRECT_ALL("Correct All The Panes",
        "Markiert die gueltigen Panes im Terminal.",
        "Marks the valid panes in the terminal."),
    NAVIGATE_MAZE("Navigate The Maze",
        "Zeigt den gueltigen Weg durch das Maze an.",
        "Shows the valid path through the maze."),
    CLICK_IN_ORDER("Click In Order",
        "Hebt die Reihenfolge der Zahlen hervor.",
        "Highlights the order of the numbers."),
    STARTS_WITH("Starts With",
        "Filtert Items nach dem geforderten Anfangsbuchstaben.",
        "Filters items by the required starting letter."),
    SELECT_ALL_COLOR("Select All Color",
        "Markiert alle Items der benoetigten Farbe.",
        "Marks all items of the required color."),
    SAME_COLOR("Change All To Same Color",
        "Hilft beim Angleichen auf eine Farbe.",
        "Helps align all items to the same color.");

    private final String title;
    private final String descriptionDe;
    private final String descriptionEn;

    TerminalSolverOption(String title, String descriptionDe, String descriptionEn) {
        this.title = title;
        this.descriptionDe = descriptionDe;
        this.descriptionEn = descriptionEn;
    }

    public String title() {
        return title;
    }

    public String description() {
        return Lang.t(descriptionDe, descriptionEn);
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
