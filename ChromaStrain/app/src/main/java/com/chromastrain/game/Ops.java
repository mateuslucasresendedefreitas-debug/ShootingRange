package com.chromastrain.game;

/**
 * The nine Field Deployments & Operations, adapted from the codex.
 * Op id = faction * 3 + slot. Slot: 0 TRIALS (dungeon), 1 RAID, 2 HUNT.
 */
public final class Ops {

    private Ops() { }

    public static final int SLOT_TRIALS = 0;
    public static final int SLOT_RAID = 1;
    public static final int SLOT_HUNT = 2;

    public static final String[] SLOT_TAG = {"TRIALS", "RAID", "HUNT"};

    public static final String[] NAME = {
            "REDHOLD ARENA",            // 0 red trials
            "THE BLEEDING DEPTHS",      // 1 red raid
            "NUKA, ORCA'S WOUND",       // 2 red hunt
            "GRAVENIGHT SPIRE",         // 3 green trials
            "TEMPLE OF THE HOLLOW SIGN",// 4 green raid
            "CLOTHWALKER",              // 5 green hunt
            "SYNAPSE SECTOR 09",        // 6 blue trials
            "HIVE INTERFACE THETA",     // 7 blue raid
            "SCION OF THE CODE SWARM",  // 8 blue hunt
    };

    public static final String[] SUB = {
            "A gladiatorial tower reactivated by rogue AI in tribute to the Den's ancestral code.",
            "Once an underwater coliseum of champions. Now a pressure-sealed tomb echoing with war cries.",
            "A dishonored champion returned from the deep — twisted, roaring, burning with shame.",
            "An old noble tower overtaken by cursed fog and phantoms of regret.",
            "A spectral labyrinth where the world forgets your presence if you stop moving.",
            "A former assassin who shed their physical form, haunting battlefield whispers.",
            "A modular testing facility caught in a recursive feedback loop.",
            "A living superstructure beneath the city, powered by neural swarm tech.",
            "A runaway prototype AI seeking to rebuild the hive in its image.",
    };

    public static final String[] MODIFIER = {
            "BERSERKER MODE — enemies enrage the longer a wave lasts",
            "PRESSURE SURGE — the sealed arena pulses with expanding shock rings",
            "RAMPAGE CYCLES — the hunt alternates fury and exhaustion",
            "FLICKERING REALITY — enemy behavior shifts every wave",
            "HOLLOW SIGN — distant enemies fade from sight",
            "HAUNTED WHISPERS — the prey is seen only while you move",
            "OVERCLOCK SURGE — everything accelerates over time; speed pays",
            "HIVE LOGIC — virus zones spread unless quarantined",
            "MIRROR PROTOCOL — the swarm studies you and echoes your weapon",
    };

    public static final String[] BOSS_NAME = {
            "VARRAK THE DEVOTED",
            "MATRON OF THE MAW",
            "NUKA, ORCA'S WOUND",
            "THYRAL, GHOST-KISSED",
            "THE ECHO PRIESTESS",
            "CLOTHWALKER",
            "ECHO-BYTE",
            "QUEEN PROCESSOR",
            "SCION OF THE CODE SWARM",
    };

    public static final String[] BOSS_HINT = {
            "Ignores ranged fire unless you press him up close — taunt, then punish.",
            "Dodge through the gaps in her echo shockwaves.",
            "Survive the rampage. Strike hard when he collapses, exhausted.",
            "In the mirrored phase he strikes from your reflection.",
            "Only one of them is real. Watch for the brighter core.",
            "He exists only while you move. Stand still and he feeds.",
            "He grows stronger every second — end it fast. Punish his dash recovery.",
            "Purge her virus zones by standing in them, or they shield her.",
            "It deploys echoes of you. Dodge your own gun.",
    };

    public static final int[] WAVES = {4, 5, 2, 4, 5, 2, 4, 5, 2};

    /** Difficulty tier per slot (hp/damage scaling). */
    public static final float[] TIER_HP = {1.0f, 1.9f, 2.9f};
    public static final float[] TIER_DMG = {0.9f, 1.3f, 1.65f};
    public static final int[] REWARD_SHARDS = {140, 260, 400};

    public static int faction(int op) {
        return op / 3;
    }

    public static int slot(int op) {
        return op % 3;
    }

    public static String lockText(int op) {
        int slot = slot(op);
        if (slot == SLOT_RAID) return "Clear " + NAME[(op / 3) * 3] + " to unlock";
        return "Clear " + NAME[(op / 3) * 3 + 1] + " to unlock";
    }
}
