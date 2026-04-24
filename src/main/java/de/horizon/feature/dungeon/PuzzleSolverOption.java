package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;

public enum PuzzleSolverOption {
    WATER_BOARD("Water Board", "Bereitet das Overlay fuer Water Board vor."),
    THREE_WEIRDOS("Three Weirdos", "Markiert die korrekte NPC-Antwort."),
    BLAZE("Higher Or Lower", "Sortiert die Blaze-Reihenfolge visuell."),
    ICE_FILL("Ice Fill", "Zeigt den geplanten Ice-Fill-Pfad."),
    QUIZ("Quiz", "Highlightet gueltige Quiz-Antworten."),
    TIC_TAC_TOE("Tic Tac Toe", "Berechnet sichere Tic-Tac-Toe-Zuege."),
    CREEPER_BEAMS("Creeper Beams", "Zeigt Creeper-Beam-Hinweise im Raum."),
    BOULDER("Boulder", "Zeigt Boulder-Puzzle-Hinweise an."),
    ICE_PATH("Ice Path", "Markiert das Ice-Path-Puzzle, sobald es eindeutig erkannt wird."),
    TELEPORT_MAZE("Teleport Maze", "Zeigt Teleport-Maze-Hinweise an.");

    private final String title;
    private final String description;

    PuzzleSolverOption(String title, String description) {
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
            case WATER_BOARD -> config.isPuzzleWaterBoardEnabled();
            case THREE_WEIRDOS -> config.isPuzzleThreeWeirdosEnabled();
            case BLAZE -> config.isPuzzleBlazeEnabled();
            case ICE_FILL -> config.isPuzzleIceFillEnabled();
            case QUIZ -> config.isPuzzleQuizEnabled();
            case TIC_TAC_TOE -> config.isPuzzleTicTacToeEnabled();
            case CREEPER_BEAMS -> config.isPuzzleCreeperBeamsEnabled();
            case BOULDER -> config.isPuzzleBoulderEnabled();
            case ICE_PATH -> config.isPuzzleIcePathEnabled();
            case TELEPORT_MAZE -> config.isPuzzleTeleportMazeEnabled();
        };
    }

    public void toggle(HorizonConfig config) {
        switch (this) {
            case WATER_BOARD -> config.setPuzzleWaterBoardEnabled(!config.isPuzzleWaterBoardEnabled());
            case THREE_WEIRDOS -> config.setPuzzleThreeWeirdosEnabled(!config.isPuzzleThreeWeirdosEnabled());
            case BLAZE -> config.setPuzzleBlazeEnabled(!config.isPuzzleBlazeEnabled());
            case ICE_FILL -> config.setPuzzleIceFillEnabled(!config.isPuzzleIceFillEnabled());
            case QUIZ -> config.setPuzzleQuizEnabled(!config.isPuzzleQuizEnabled());
            case TIC_TAC_TOE -> config.setPuzzleTicTacToeEnabled(!config.isPuzzleTicTacToeEnabled());
            case CREEPER_BEAMS -> config.setPuzzleCreeperBeamsEnabled(!config.isPuzzleCreeperBeamsEnabled());
            case BOULDER -> config.setPuzzleBoulderEnabled(!config.isPuzzleBoulderEnabled());
            case ICE_PATH -> config.setPuzzleIcePathEnabled(!config.isPuzzleIcePathEnabled());
            case TELEPORT_MAZE -> config.setPuzzleTeleportMazeEnabled(!config.isPuzzleTeleportMazeEnabled());
        }
    }
}
