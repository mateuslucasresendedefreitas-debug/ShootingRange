package com.chromastrain.game;

/**
 * The three Chromanite strains / factions, adapted 1:1 from the design codex
 * ("Codex: Skills, Gadgets, Weapons and Operations").
 * Index: 0 = Crimson Den (Red Core), 1 = Verdant Herd (Green Bloom),
 * 2 = Navy Colony (Blue Cluster).
 */
public final class Strain {

    private Strain() { }

    public static final int RED = 0;
    public static final int GREEN = 1;
    public static final int BLUE = 2;

    public static final String[] FACTION = {"CRIMSON DEN", "VERDANT HERD", "NAVY COLONY"};
    public static final String[] STRAIN = {"Red Core", "Green Bloom", "Blue Cluster"};
    public static final String[] CLASS_LINE = {
            "Strength · Domination · Resilience",
            "Agility · Precision · Willpower",
            "Adaptability · Focus · Willpower"};
    public static final String[] ROLE = {"VANGUARD", "PHANTOM", "SAVANT"};
    public static final String[] TAGLINE = {
            "Raw force. Thrives on fire, tension and violent resonance.",
            "Reflex, escape and misdirection. Favored by scouts and saboteurs.",
            "Precise pressure at the perfect moment. Powerful, quiet, cold."};

    public static int color(int f) {
        return f == RED ? Palette.RED : (f == GREEN ? Palette.GREEN : Palette.BLUE);
    }

    public static int colorDark(int f) {
        return f == RED ? Palette.RED_DARK : (f == GREEN ? Palette.GREEN_DARK : Palette.BLUE_DARK);
    }

    public static int glow(int f) {
        return f == RED ? Palette.RED_GLOW : (f == GREEN ? Palette.GREEN_GLOW : Palette.BLUE_GLOW);
    }

    // ------------------------------------------------------------- weapons

    public static final String[] GUN_NAME = {"EMBERMAW", "NEEDLEWRAITH", "GLACIVORE"};
    public static final String[] GUN_TYPE = {
            "Assault Rifle (Semi-Auto)", "Burst SMG (Silenced)", "Cryo-Energy Sniper"};
    public static final String[] GUN_PASSIVE = {
            "Heat Vent Cycle — every 4th shot is a piercing incendiary round (burns 4s)",
            "Neurofracture Rounds — every 3rd burst applies Neural Disrupt (slow -10%, 5s)",
            "Ice Latch — shots stack Chill (3 max); at max: Freeze + crit vulnerability"};

    // dmg min/max, shots per second, projectile speed (u/s), spread (rad)
    public static final float[][] GUN = {
            // dmgMin, dmgMax, rate, speed, spread, burst
            {164, 202, 2.4f, 1150, 0.030f, 1},
            {62, 76, 1.55f, 1250, 0.075f, 3},   // rate = bursts/sec
            {198, 225, 1.05f, 1600, 0.008f, 1},
    };

    public static final String[] MELEE_NAME = {"FURYBRAND", "WHISPERFANGS", "EVOCLASM GAUNTLETS"};
    public static final String[] MELEE_TYPE = {
            "Greatblade (Heavy)", "Twin Daggers (Very Fast)", "Tech Gauntlets (Combo)"};
    public static final String[] MELEE_PASSIVE = {
            "Bloodfire Memory — bonus power as HP drops; low HP strikes can ignite",
            "Bloodthread — crits stack Bleed (5 max); max stacks cause Hemorrhage",
            "System Overclock — +3% damage per active buff; finisher stuns"};

    // dmgMin, dmgMax, swings/sec, range (u), arc (rad)
    public static final float[][] MELEE = {
            {221, 256, 1.05f, 180, 2.4f},
            {148, 164, 2.4f, 150, 1.7f},
            {162, 178, 1.7f, 155, 2.0f},
    };

    // ------------------------------------------------------------- skills

    public static final String[] SKILL_NAME = {"SEISMIC FIST", "PHANTOM VEIN", "OVERCHARGE WAVE"};
    public static final String[] SKILL_DESC = {
            "Ground slam: damages and staggers everything in a wide radius. Burning enemies take bonus damage.",
            "Cloak for 4s (attacking breaks it). First strike from stealth always crits. Exiting grants +50% speed.",
            "Electric shockwave: interrupts and disorients enemies, weakening their damage. Refunds gadget cooldown."};
    public static final float[] SKILL_CD = {14f, 16f, 15f};

    // ------------------------------------------------------------- gadgets

    public static final String[] GADGET_NAME = {"RED SOLVENT FLASK", "PULSE DECOY", "COGNITIVE LOOP TRAP"};
    public static final String[] GADGET_DESC = {
            "Thrown flask ignites a burning zone for 6s. Seismic Fist grows +25% inside the flames.",
            "Projects a decoy that taunts enemies for 4s, then detonates in a slowing flash.",
            "Deploys a field that zaps and staggers enemies inside it every second."};
    public static final float[] GADGET_CD = {11f, 12f, 13f};

    // ------------------------------------------------------------- doses

    public static final String[] DOSE_NAME = {"ADRENALINE PUMP", "BLOOM RUSH", "CORTEX SPLIT"};
    public static final String[] DOSE_DESC = {
            "Red Core dose: +40% damage, harder to stagger, melee steals life. Withdrawal: slowed.",
            "Green Bloom dose: every hit crits, +25% speed, brief afterimages. Withdrawal: slowed fire rate.",
            "Blue Cluster dose: time dilates — the world slows while you keep pace. Withdrawal: skills lock."};

    // ------------------------------------------------------------- passives

    public static final String[] PASSIVE_NAME = {"PAIN ECHO", "SPINAL BLOOM", "DATA CHARGE"};
    public static final String[] PASSIVE_DESC = {
            "Below 30% HP, taking a hit releases a retaliatory shockwave (5s cooldown).",
            "Untouched for 3s: +12% speed and the next hit you take is softened.",
            "Kills grant stacking +3% damage charges (5 max, 6s)."};

    // ---------------------------------------------------- growth conditions
    // From the codex: a strain blooms when at least two of the three stimulus
    // TYPES are present. [faction][category][items]; category 0 Bio, 1 Energy, 2 Acoustic.

    public static final String[] CATEGORY = {"BIOCHEMICAL", "ENERGY", "ACOUSTIC"};

    public static final String[][][] GROWTH = {
            { // Red Core
                {"Adrenaline", "Endorphin", "Potassium", "Hydrogen"},
                {"Heat", "Radiation", "Friction", "High Pressure"},
                {"Bass Frequencies", "Machinery", "Explosions"},
            },
            { // Green Bloom
                {"Norepinephrine", "Melatonin", "Vanadium", "Tungsten"},
                {"Mild Temperature", "Mild Pressure", "Low-Light"},
                {"Steady Frequency", "Subtle Buzz", "Complete Silence"},
            },
            { // Blue Cluster
                {"Serotonin", "Oxytocin", "Lithium", "Cobalt"},
                {"Low Temperature", "High Pressure", "Electricity"},
                {"EM Signatures", "Frequent Beats", "Water"},
            },
    };

    /** Returns faction index owning this stimulus, or -1. */
    public static int stimulusOwner(String s) {
        for (int f = 0; f < 3; f++) {
            for (int c = 0; c < 3; c++) {
                String[] arr = GROWTH[f][c];
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i].equals(s)) return f;
                }
            }
        }
        return -1;
    }

    /** Category of a stimulus within a faction, or -1. */
    public static int stimulusCategory(int f, String s) {
        for (int c = 0; c < 3; c++) {
            String[] arr = GROWTH[f][c];
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].equals(s)) return c;
            }
        }
        return -1;
    }
}
