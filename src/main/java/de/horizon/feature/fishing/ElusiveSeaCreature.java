package de.horizon.feature.fishing;

public enum ElusiveSeaCreature {
    FROG_PRINCE("Frog Prince", "frog_prince", "bow down before the frog prince"),
    THE_LOCH_EMPEROR("The Loch Emperor", "loch_emperor", "loch emperor arises"),
    NESSIE("Nessie", "nessie", "could it be... nessie"),
    WATER_HYDRA("Water Hydra", "water_hydra", "water hydra has come to test"),
    PUDDLE_JUMPER("Puddle Jumper", "puddle_jumper", "preparing for liftoff"),
    ALLIGATOR("Alligator", "alligator", "it's an alligator"),
    AGARIMOO("Agarimoo", "agarimoo", "chumcap bucket trembles"),
    WATER_WORM("Water Worm", "water_worm", "a water worm surfaces"),
    ABYSSAL_MINER("Abyssal Miner", "abyssal_miner", "abyssal miner breaks out"),
    THUNDER("Thunder", "thunder", "rumble as thunder emerges"),
    FIERY_SCUTTLER("Fiery Scuttler", "fiery_scuttler", "fiery scuttler inconspicuously"),
    LORD_JAWBUS("Lord Jawbus", "lord_jawbus", "lord jawbus has arrived"),
    PHANTOM_FISHER("Phantom Fisher", "phantom_fisher", "phantom fisher has come to haunt"),
    GRIM_REAPER("Grim Reaper", "grim_reaper", "manifestation of death himself"),
    GREAT_WHITE_SHARK("Great White Shark", "great_white_shark", "great white shark has tracked"),
    BLUE_RINGED_OCTOPUS("Blue Ringed Octopus", "blue_ringed_octopus", "it's a blue ringed octopus"),
    WIKI_TIKI("Wiki Tiki", "wiki_tiki", "disturbed the wiki tiki"),
    RAGNAROK("Ragnarok", "ragnarok", "ragnarok is here"),
    PLHLEGBLAST("Plhlegblast", "plhlegblast", "a plhlegblast appeared");

    private final String displayName;
    private final String id;
    /** Unique spawn message fragment for creatures that don't use "caught a/an" format. Lowercase. Null = standard format. */
    private final String spawnMessageFragment;

    ElusiveSeaCreature(String displayName, String id, String spawnMessageFragment) {
        this.displayName = displayName;
        this.id = id;
        this.spawnMessageFragment = spawnMessageFragment;
    }

    public String displayName() { return displayName; }
    public String id() { return id; }
    public String spawnMessageFragment() { return spawnMessageFragment; }
}
