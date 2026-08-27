package com.chromastrain.game;

import android.graphics.Color;

/** Central color identity: neon bio-mineral on deep void. */
public final class Palette {

    private Palette() { }

    // Void / background
    public static final int VOID_TOP = Color.rgb(11, 14, 26);
    public static final int VOID_BOT = Color.rgb(4, 6, 12);
    public static final int GRID = Color.argb(26, 120, 180, 255);
    public static final int PANEL = Color.argb(216, 13, 18, 32);
    public static final int PANEL_EDGE = Color.argb(90, 130, 200, 255);
    public static final int INK = Color.rgb(226, 236, 248);
    public static final int INK_DIM = Color.rgb(140, 152, 176);
    public static final int GOLD = Color.rgb(255, 205, 96);
    public static final int DANGER = Color.rgb(255, 82, 92);

    // Faction identities
    public static final int RED = Color.rgb(255, 74, 82);
    public static final int RED_DARK = Color.rgb(122, 22, 30);
    public static final int RED_GLOW = Color.argb(150, 255, 84, 70);

    public static final int GREEN = Color.rgb(84, 255, 158);
    public static final int GREEN_DARK = Color.rgb(16, 106, 62);
    public static final int GREEN_GLOW = Color.argb(150, 84, 255, 158);

    public static final int BLUE = Color.rgb(92, 168, 255);
    public static final int BLUE_DARK = Color.rgb(24, 62, 128);
    public static final int BLUE_GLOW = Color.argb(150, 92, 168, 255);

    public static final int HP = Color.rgb(255, 96, 110);
    public static final int HP_BACK = Color.argb(140, 60, 16, 24);
    public static final int DOSE = Color.rgb(214, 120, 255);
    public static final int SHARD = Color.rgb(150, 232, 255);

    public static int mix(int a, int b, float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a), aa = Color.alpha(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b), ba = Color.alpha(b);
        return Color.argb(
                (int) (aa + (ba - aa) * t),
                (int) (ar + (br - ar) * t),
                (int) (ag + (bg - ag) * t),
                (int) (ab + (bb - ab) * t));
    }

    public static int withAlpha(int c, int alpha) {
        return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
    }
}
