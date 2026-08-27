package com.chromastrain.game;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent progression. Thin wrapper over SharedPreferences. */
public class Save {

    private final SharedPreferences sp;

    // settings
    public boolean sfx = true;
    public boolean music = true;
    public boolean haptics = true;

    // meta
    public int shards;          // currency
    public int nodes;           // raw chromanite nodes (lab material)
    public int faction;         // 0 red 1 green 2 blue
    public boolean tutorialSeen;

    // upgrades (levels 0..5): vitality, power, reflex, systems
    public final int[] up = new int[4];

    // per-op: cleared flag + best score  (op ids 0..8)
    public final boolean[] cleared = new boolean[9];
    public final int[] best = new int[9];

    // stabilized doses per faction (start runs with full dose meter)
    public final int[] doses = new int[3];

    // codex entries read (ids up to 32)
    public final boolean[] codexRead = new boolean[32];

    public Save(Context ctx) {
        sp = ctx.getSharedPreferences("chromastrain", Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        sfx = sp.getBoolean("sfx", true);
        music = sp.getBoolean("music", true);
        haptics = sp.getBoolean("haptics", true);
        shards = sp.getInt("shards", 0);
        nodes = sp.getInt("nodes", 0);
        faction = sp.getInt("faction", 0);
        tutorialSeen = sp.getBoolean("tut", false);
        for (int i = 0; i < up.length; i++) up[i] = sp.getInt("up" + i, 0);
        for (int i = 0; i < 9; i++) {
            cleared[i] = sp.getBoolean("cl" + i, false);
            best[i] = sp.getInt("best" + i, 0);
        }
        for (int i = 0; i < 3; i++) doses[i] = sp.getInt("dose" + i, 0);
        for (int i = 0; i < codexRead.length; i++) codexRead[i] = sp.getBoolean("cx" + i, false);
    }

    public void flush() {
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean("sfx", sfx);
        e.putBoolean("music", music);
        e.putBoolean("haptics", haptics);
        e.putInt("shards", shards);
        e.putInt("nodes", nodes);
        e.putInt("faction", faction);
        e.putBoolean("tut", tutorialSeen);
        for (int i = 0; i < up.length; i++) e.putInt("up" + i, up[i]);
        for (int i = 0; i < 9; i++) {
            e.putBoolean("cl" + i, cleared[i]);
            e.putInt("best" + i, best[i]);
        }
        for (int i = 0; i < 3; i++) e.putInt("dose" + i, doses[i]);
        for (int i = 0; i < codexRead.length; i++) e.putBoolean("cx" + i, codexRead[i]);
        e.apply();
    }

    /** Highest op unlocked for a faction: trial always; raid after trial; hunt after raid. */
    public boolean opUnlocked(int opId) {
        int slot = opId % 3; // 0 trial, 1 raid, 2 hunt (per faction block)
        if (slot == 0) return true;
        int factionBase = (opId / 3) * 3;
        return cleared[factionBase + slot - 1];
    }
}
