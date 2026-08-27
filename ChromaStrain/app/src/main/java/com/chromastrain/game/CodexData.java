package com.chromastrain.game;

/**
 * In-game codex — the source design document ("Codex: Skills, Gadgets, Weapons
 * and Operations") condensed into readable field entries. Reading an entry for
 * the first time grants a Field Study bonus, and the LAB quiz answers all come
 * from the strain profiles here.
 */
public final class CodexData {

    private CodexData() { }

    public static final String[] TITLE = {
            "CHROMANITE — OVERVIEW",             // 0
            "RED CORE — STRAIN PROFILE",         // 1
            "GREEN BLOOM — STRAIN PROFILE",      // 2
            "BLUE CLUSTER — STRAIN PROFILE",     // 3
            "CRIMSON DEN — COMBAT SKILLS",       // 4
            "VERDANT HERD — COMBAT SKILLS",      // 5
            "NAVY COLONY — COMBAT SKILLS",       // 6
            "CRIMSON DEN — GADGETS",             // 7
            "VERDANT HERD — GADGETS",            // 8
            "NAVY COLONY — GADGETS",             // 9
            "EMBERMAW & FURYBRAND",              // 10
            "NEEDLEWRAITH & WHISPERFANGS",       // 11
            "GLACIVORE & EVOCLASM",              // 12
            "CRIMSON OPERATIONS",                // 13
            "VERDANT OPERATIONS",                // 14
            "NAVY OPERATIONS",                   // 15
            "CULTIVATION PRIMER",                // 16
            "FORGED SETS",                       // 17
    };

    public static final int[] FACTION = {
            -1, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, -1, -1
    };

    public static final String[] BODY = {
            // 0 overview
            "Category: Substance / Bio-Mineral Ecosystem.\n\n"
            + "Originally found deep underground during an excavation, Chromanite was thought "
            + "to be an inert crystal. Upon exposure to air and organic matter it 'awoke', "
            + "sprouting tendrils and forming an intricate root-web.\n\n"
            + "Chromanite is no longer viewed as a mineral, but as a living ecosystem of "
            + "bio-mineral colonies — like fungi, coral reefs or mycelium networks. Each node "
            + "is a symbiotic life form that feeds off the environmental conditions of the land "
            + "it grows on. It is volatile and adaptive, interacting with anything in the "
            + "periodic table.\n\n"
            + "Each harvested node, through chemical stabilization, can be refined into a "
            + "drug-compatible form. A dose activates an override in the user's genetic "
            + "expression through a burst of encoded protein chains, mimicking dormant "
            + "evolutionary paths — short-term superhuman feats, highly specialized by the "
            + "refinement method. The body is forced into a reset afterward (withdrawal). "
            + "Repeated use alters physiology, creating dependency.\n\n"
            + "The government controls the purest Chromanite; the three factions developed "
            + "adaptive recipes after the war using fragments of the original source.",

            // 1 red core
            "Strain Class: Strength, Domination & Resilience.\n\n"
            + "Red Core is grown through intense physical and environmental stress. Cultivated "
            + "by the Crimson Den, it is harvested in controlled warzone conditions involving "
            + "heat, trauma and vibration. Refined, it enhances physical power, impact "
            + "tolerance and aggression. Red Core is Chromanite's response to raw force — it "
            + "thrives on fire, tension and violent resonance.\n\n"
            + "GROWTH CONDITIONS — blooms when at least two of these stimulus TYPES are "
            + "present:\n"
            + "· Biochemical: Adrenaline, Endorphin, Potassium, Hydrogen\n"
            + "· Energy: Heat, Radiation, Friction, High Pressure\n"
            + "· Acoustic: Bass Frequencies, Machinery, Explosions\n\n"
            + "Doses push the body beyond natural safety limits. Best used in close-range, "
            + "high-intensity combat.",

            // 2 green bloom
            "Strain Class: Agility, Precision & Willpower.\n\n"
            + "Green Bloom is adapted to tension, evasion and environmental subtlety. "
            + "Cultivated by the Verdant Herd, it thrives in low-stimulation zones where the "
            + "body enters a heightened sensory state. Its effects emphasize reflex, escape, "
            + "misdirection and the manipulation of perception.\n\n"
            + "GROWTH CONDITIONS — blooms when at least two of these stimulus TYPES are "
            + "present:\n"
            + "· Biochemical: Norepinephrine, Melatonin, Vanadium, Tungsten\n"
            + "· Energy: Mild Temperature, Mild Pressure, Low-Light\n"
            + "· Acoustic: Steady Frequency, Subtle Buzz, Complete Silence\n\n"
            + "Refined doses activate tension-responsive proteins that enhance neural focus, "
            + "motor coordination and situational awareness. Favored by scouts, saboteurs and "
            + "covert operatives.",

            // 3 blue cluster
            "Strain Class: Adaptability, Focus & Willpower.\n\n"
            + "Blue Cluster represents the Navy Colony's pursuit of control through clarity, "
            + "calculation and mental dominance. Developed in subterranean zones saturated "
            + "with electromagnetic feedback, it enhances perception, internal regulation and "
            + "psionic projection. Not a strain of brute force — precise pressure applied at "
            + "the perfect moment. Its effects reflect the stillness of deep water: powerful, "
            + "quiet and cold.\n\n"
            + "GROWTH CONDITIONS — blooms when at least two of these stimulus TYPES are "
            + "present:\n"
            + "· Biochemical: Serotonin, Oxytocin, Lithium, Cobalt\n"
            + "· Energy: Low Temperature, High Pressure, Electricity\n"
            + "· Acoustic: EM Signatures, Frequent Beats, Water\n\n"
            + "Users gain enhanced sensory processing, multitask execution and emotion "
            + "suppression. Reactions become automated. Focus replaces fear. Will overrides "
            + "fatigue.",

            // 4 red skills
            "ADRENALINE PUMP (Dose) — Self buff synthesized from stimulant-rich Chromanite "
            + "harvested in blood-saturated zones. Veins glow red; heat shimmer rises off "
            + "skin.\n\n"
            + "SEISMIC FIST — Area smash. Ground slam that damages and staggers enemies in a "
            + "wide radius, amplified by kinetic force and thermal exposure — burning targets "
            + "take extra damage, and the slam grows inside flame zones.\n\n"
            + "PAIN ECHO — Passive/reactive. A defense developed from trauma-fed Chromanite "
            + "clusters: at low HP, taking a hit releases a retaliatory shockwave burst from "
            + "the torso.",

            // 5 green skills
            "PHANTOM VEIN — Stealth toggle. Green Bloom distorts light refraction and dampens "
            + "external noise through bio-reactive emission. Attacking breaks the cloak; "
            + "exiting grants a burst of speed, and the first strike from stealth always "
            + "lands true.\n\n"
            + "SYNAPSE SURGE — Reflex buff. Enhanced norepinephrine sensitivity allows "
            + "hyper-reflexive evasion during high-alert states.\n\n"
            + "SPINAL BLOOM — Passive. Biofeedback along the vertebrae accelerates neural "
            + "readiness: after avoiding damage briefly, movement quickens and the next hit "
            + "taken is softened.",

            // 6 blue skills
            "CORTEX SPLIT (Dose) — Split attention between amplification modes; twin data "
            + "rings orbit the user's head. In the field this manifests as time dilation — "
            + "the world slows while the user keeps pace.\n\n"
            + "OVERCHARGE WAVE — AoE disrupt. Emits an electric shockwave that disrupts "
            + "enemy actions, disorients their vision and weakens their output.\n\n"
            + "NEURAL LOCK — Precision CC. An electric-blue thread lashes out to lock an "
            + "enemy's actions — refined in the field into the freeze cascade of the "
            + "Glacivore's Ice Latch.",

            // 7 red gadgets
            "RED SOLVENT FLASK — Thrown fire gadget. Creates a burning zone for 6 seconds. "
            + "Seismic Fist gains +25% radius inside the flame.\n"
            + "Crafting: Diluted Chromanite + Alcoholic Solvent + Rag Fuse.\n\n"
            + "SHOCKSPIKE CANISTER — Pressure mine. Stuns enemies on proximity.\n"
            + "Crafting: Cracked Chromanite Core + Shrapnel + Pressure Sensor.\n\n"
            + "THERMAL CHARGE — Directional explosive. Emits a thermal cone that shreds "
            + "armor and breaches structures.\n"
            + "Crafting: Refined Red Core Dust + Thermite Paste + Magnetic Clamp.",

            // 8 green gadgets
            "MIST CAPSULE — Vision disruptor. A smoke field that blocks vision and weakens "
            + "enemy lock-on; grants brief stealth on entry.\n"
            + "Crafting: Bloom Extract + Coolant Capsule + Diffuser Mesh.\n\n"
            + "PULSE DECOY — Holographic distractor. Projects an illusion of your last "
            + "movement; on destruction it emits a flash that slows enemies nearby.\n"
            + "Crafting: Holo-core + Bloom Tissue Sample + Charge Cell.\n\n"
            + "SCENTBREAKER POD — Dispel. A burst of pheromone-neutralizing gas that removes "
            + "tracking effects and reduces threat.\n"
            + "Crafting: Bloom Gland + Coolant Gel + Alloy Mesh.",

            // 9 blue gadgets
            "FOCUS SHARD — Instant recharge. Consume a charged shard to accelerate all "
            + "cooldowns.\n"
            + "Crafting: Cryo Crystal Core + Stabilizer Gel + Sync Node.\n\n"
            + "COGNITIVE LOOP TRAP — Field trap. Creates a field that turns an enemy's own "
            + "actions against them, zapping and staggering everything inside.\n"
            + "Crafting: Neural Mirror + Coldroot Lattice + Pulse Sink.\n\n"
            + "DISSONANCE SPIKE — Debuff dart. Marks a target, slowing their casts; failed "
            + "actions burn them out.\n"
            + "Crafting: Ionized Spike + Cobalt Ink + Bloom Casing.",

            // 10 red weapons
            "EMBERMAW — Assault Rifle (Semi-Automatic). Base damage 164–202, effective range "
            + "30–70m.\n"
            + "Passive, HEAT VENT CYCLE: every 4th shot becomes a piercing incendiary round, "
            + "burning all targets it passes through for 4s.\n"
            + "The barrel is heavy and vented, glowing slits pulse red-orange as heat builds; "
            + "each vented shot sounds like an industrial pressure release.\n\n"
            + "FURYBRAND — Greatblade (Heavy Melee). Base damage 221–256, +20% stagger.\n"
            + "Passive, BLOODFIRE MEMORY: attack power rises as HP drops; at low HP, strikes "
            + "can ignite enemies.\n"
            + "Forged from fused blades of fallen warriors bound by solidified molten core "
            + "veins; the central groove pulses with magma-like glow, flaking embers with "
            + "each swing.",

            // 11 green weapons
            "NEEDLEWRAITH — Burst SMG (Silenced). 3-round bursts, effective range 15–40m.\n"
            + "Passive, NEUROFRACTURE ROUNDS: every 3rd burst applies Neural Disrupt, "
            + "degrading enemy accuracy and speed for 5s.\n"
            + "Lightweight frame with curved organic panels; the magazine pulses with faint "
            + "bioluminescent glow. Sounds like snapping muscle fibers and low hisses of "
            + "air pressure.\n\n"
            + "WHISPERFANGS — Twin Daggers (Very Fast). Base damage 148–164, bleed-focused.\n"
            + "Passive, BLOODTHREAD: critical hits apply Bleed (5 stacks max). At max stacks "
            + "the target Hemorrhages, taking increased damage from all sources.\n"
            + "Forged from fractured jadestone on a steel frame; light trails shimmer on "
            + "strike, like slicing through mist.",

            // 12 blue weapons
            "GLACIVORE — Cryo-Energy Sniper Rifle. Base damage 198–225, effective range "
            + "60–120m.\n"
            + "Passive, ICE LATCH: shots apply stacking Chill (3 max). At max stacks the "
            + "target Freezes, interrupting its action and exposing it to critical hits.\n"
            + "The barrel channels cryo energy through coiled-glass veins; muzzle flash "
            + "crystallizes briefly in the air.\n\n"
            + "EVOCLASM GAUNTLETS — Tech Gauntlets. Base damage 162–178.\n"
            + "Passive, SYSTEM OVERCLOCK: +3% damage per active buff; the combo finisher "
            + "stuns and disables shielded enemies.\n"
            + "Sleek gauntlets with pulsing blue light nodes. 'Prototype brawler interface "
            + "powered by unstable AI cores. Banned in four areas for excessive force.'",

            // 13 red ops
            "DUNGEON — REDHOLD ARENA: a gladiatorial tower reactivated by rogue AI in "
            + "tribute to the Crimson Den's ancestral code. Modifier 'Berserker Mode' — "
            + "enemies grow stronger the longer they go uninterrupted. Final floor: VARRAK "
            + "THE DEVOTED, who ignores all damage unless taunted consistently — a test of "
            + "control and timing.\n\n"
            + "RAID — THE BLEEDING DEPTHS: once an underwater coliseum of champions, now a "
            + "pressure-sealed tomb echoing with war cries. Final boss: MATRON OF THE MAW, "
            + "wearing a whale-skull mask, emitting massive echo shockwaves that demand "
            + "precise movement.\n\n"
            + "BOSS HUNT — NUKA, ORCA'S WOUND: a dishonored champion who returned from the "
            + "deep, twisted and burning with shame. Attacks in cycles of uncontrollable "
            + "rage and exhausted collapse; coral-encrusted tusks, orca stripes glowing red "
            + "in his berserk state.",

            // 14 green ops
            "DUNGEON — GRAVENIGHT SPIRE: an old noble tower overtaken by cursed fog and "
            + "phantoms of regret. Modifier 'Flickering Reality' — enemy behavior shifts "
            + "constantly. Final floor: THYRAL, GHOST-KISSED, whose second phase inverts the "
            + "fight into a mirrored realm.\n\n"
            + "RAID — TEMPLE OF THE HOLLOW SIGN: a spectral labyrinth where the world "
            + "forgets your presence if you stop moving. Boss: THE ECHO PRIESTESS, who hides "
            + "among clones each phase — killing the wrong one empowers her.\n\n"
            + "BOSS HUNT — CLOTHWALKER: a former assassin who shed their physical form "
            + "entirely. Only appears during action — if you stop, Clothwalker vanishes and "
            + "resets. Tattered rags, semi-invisible unless revealed by motion.",

            // 15 blue ops
            "DUNGEON — SYNAPSE SECTOR 09: a modular testing facility built by Navy Colony "
            + "scientists, now on a recursive feedback loop. Modifier 'Overclock Surge' — "
            + "everything accelerates; rewards scale with speed and precision. Final floor: "
            + "ECHO-BYTE, who gains power the longer you take.\n\n"
            + "RAID — HIVE INTERFACE THETA: a living superstructure beneath the city powered "
            + "by neural swarm tech. Boss: QUEEN PROCESSOR, who spreads virus nodes that "
            + "must be quarantined zone by zone.\n\n"
            + "BOSS HUNT — SCION OF THE CODE SWARM: a runaway prototype AI seeking to "
            + "rebuild the hive in its image. Summons digital clones of its prey — you will "
            + "have to dodge your own gun.",

            // 16 cultivation primer
            "Chromanite can seed and grow new clusters over time when properly 'fed' by the "
            + "environment. Different conditions produce different strains, characterized by "
            + "a distinct color shift. Faction-controlled zones use stimulus farming methods "
            + "to grow their own versions.\n\n"
            + "THE BLOOM RULE: a strain blooms when at least TWO of its three stimulus TYPES "
            + "(Biochemical, Energy, Acoustic) are present. Feeding a culture two stimuli "
            + "from the SAME type is not enough — diversity of conditions is what wakes the "
            + "colony.\n\n"
            + "In the LAB, spend raw nodes harvested in the field to run cultivations: pick "
            + "two stimuli that both belong to the target strain, from two different types. "
            + "Correct cultivations refine STABILIZED DOSES — you deploy on your next "
            + "operation already dosed. The strain profiles in this codex list every valid "
            + "stimulus. Study them.",

            // 17 forged sets
            "Champions who survive a faction's BOSS HUNT return changed. The armorers "
            + "study what the hunt did to their gear and bind it into a permanent set — "
            + "the hunt is the forge.\n\n"
            + "ASHBLOOD FORGE (Crimson Den) — quenched in Nuka's rage. +10% burn damage; "
            + "kills on burning enemies vent a second off Seismic Fist's cycle.\n\n"
            + "JADESTONE WARRIOR (Verdant Herd) — cut from the Clothwalker's silence. "
            + "Bleeds last 25% longer, and dagger crits spread Bleed to enemies nearby.\n\n"
            + "MIND'S ANCHOR (Navy Colony) — annealed against the Code Swarm. Chilled "
            + "targets take +12% damage; Frozen targets take +15%.\n\n"
            + "A forged set is permanent and always active while running its strain.",
    };

    public static final int COUNT = TITLE.length;
    public static final int READ_BONUS = 10;
}
