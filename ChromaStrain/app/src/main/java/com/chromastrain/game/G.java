package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.Random;

/** Shared toolkit: rng, math, text and glow drawing. All coordinates in virtual units. */
public final class G {

    private G() { }

    public static final Random RNG = new Random();

    public static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Paint TXT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF TMP = new RectF();
    private static final Path TMP_PATH = new Path();

    public static Typeface FONT_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    public static Typeface FONT = Typeface.SANS_SERIF;
    public static Typeface FONT_MONO = Typeface.MONOSPACE;

    public static float rnd() {
        return RNG.nextFloat();
    }

    public static float rnd(float a, float b) {
        return a + RNG.nextFloat() * (b - a);
    }

    public static int rndi(int a, int b) { // inclusive
        return a + RNG.nextInt(b - a + 1);
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float len(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        return len(x2 - x1, y2 - y1);
    }

    public static float angleTo(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    /** Moves an angle toward another by max step, wrapping properly. */
    public static float turnToward(float a, float target, float step) {
        float d = target - a;
        while (d > Math.PI) d -= (float) (Math.PI * 2);
        while (d < -Math.PI) d += (float) (Math.PI * 2);
        if (d > step) d = step;
        if (d < -step) d = -step;
        return a + d;
    }

    // ------------------------------------------------------------------ text

    public static void text(Canvas c, String s, float x, float y, float size,
                            int color, Paint.Align align, Typeface tf) {
        TXT.setTypeface(tf);
        TXT.setTextSize(size);
        TXT.setColor(color);
        TXT.setTextAlign(align);
        TXT.setShadowLayer(size * 0.10f, 0, size * 0.05f, 0xC0000000);
        c.drawText(s, x, y, TXT);
        TXT.clearShadowLayer();
    }

    public static void text(Canvas c, String s, float x, float y, float size, int color) {
        text(c, s, x, y, size, color, Paint.Align.LEFT, FONT);
    }

    public static void textC(Canvas c, String s, float x, float y, float size, int color) {
        text(c, s, x, y, size, color, Paint.Align.CENTER, FONT);
    }

    public static void textCB(Canvas c, String s, float x, float y, float size, int color) {
        text(c, s, x, y, size, color, Paint.Align.CENTER, FONT_BOLD);
    }

    public static void textB(Canvas c, String s, float x, float y, float size, int color) {
        text(c, s, x, y, size, color, Paint.Align.LEFT, FONT_BOLD);
    }

    public static void textR(Canvas c, String s, float x, float y, float size, int color) {
        text(c, s, x, y, size, color, Paint.Align.RIGHT, FONT);
    }

    public static float textWidth(String s, float size, Typeface tf) {
        TXT.setTypeface(tf);
        TXT.setTextSize(size);
        return TXT.measureText(s);
    }

    /** Simple word wrap; returns number of lines drawn. */
    public static int textWrap(Canvas c, String s, float x, float y, float size,
                               int color, float maxWidth, float lineH, Typeface tf) {
        TXT.setTypeface(tf);
        TXT.setTextSize(size);
        TXT.setColor(color);
        TXT.setTextAlign(Paint.Align.LEFT);
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            if (w.contains("\n")) {
                String[] parts = w.split("\n", -1);
                for (int p = 0; p < parts.length; p++) {
                    if (p > 0) {
                        if (c != null) c.drawText(line.toString(), x, y + lines * lineH, TXT);
                        lines++;
                        line.setLength(0);
                    }
                    appendWord(c, line, parts[p], x, y, size, maxWidth, lineH, lines);
                    lines = lastLines;
                }
                continue;
            }
            appendWord(c, line, w, x, y, size, maxWidth, lineH, lines);
            lines = lastLines;
        }
        if (line.length() > 0) {
            if (c != null) c.drawText(line.toString(), x, y + lines * lineH, TXT);
            lines++;
        }
        return lines;
    }

    private static int lastLines;

    private static void appendWord(Canvas c, StringBuilder line, String w, float x, float y,
                                   float size, float maxWidth, float lineH, int lines) {
        if (w.length() == 0) {
            lastLines = lines;
            return;
        }
        String candidate = line.length() == 0 ? w : line + " " + w;
        if (TXT.measureText(candidate) <= maxWidth || line.length() == 0) {
            line.setLength(0);
            line.append(candidate);
        } else {
            if (c != null) c.drawText(line.toString(), x, y + lines * lineH, TXT);
            lines++;
            line.setLength(0);
            line.append(w);
        }
        lastLines = lines;
    }

    // ------------------------------------------------------------------ shapes

    public static void circle(Canvas c, float x, float y, float r, int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        c.drawCircle(x, y, r, P);
    }

    public static void ring(Canvas c, float x, float y, float r, float stroke, int color) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(stroke);
        P.setColor(color);
        c.drawCircle(x, y, r, P);
        P.setStyle(Paint.Style.FILL);
    }

    /** Soft additive-looking glow via radial gradient (no blur filters — fast). */
    public static void glow(Canvas c, float x, float y, float r, int color) {
        P.setStyle(Paint.Style.FILL);
        int core = Palette.withAlpha(color, Math.min(230, android.graphics.Color.alpha(color) + 60));
        P.setShader(new RadialGradient(x, y, r, core, Palette.withAlpha(color, 0), Shader.TileMode.CLAMP));
        c.drawCircle(x, y, r, P);
        P.setShader(null);
    }

    public static void rr(Canvas c, float l, float t, float r, float b, float rad, int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        TMP.set(l, t, r, b);
        c.drawRoundRect(TMP, rad, rad, P);
    }

    public static void rrStroke(Canvas c, float l, float t, float r, float b, float rad,
                                float stroke, int color) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(stroke);
        P.setColor(color);
        TMP.set(l, t, r, b);
        c.drawRoundRect(TMP, rad, rad, P);
        P.setStyle(Paint.Style.FILL);
    }

    /** Crystal shard polygon (the game's signature shape). */
    public static void shard(Canvas c, float x, float y, float w, float h, float ang,
                             int fill, int edge) {
        TMP_PATH.reset();
        float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
        float[] px = {0, 0.42f, 0.30f, 0, -0.30f, -0.42f};
        float[] py = {-0.52f, -0.18f, 0.42f, 0.52f, 0.42f, -0.18f};
        for (int i = 0; i < 6; i++) {
            float lx = px[i] * w, ly = py[i] * h;
            float rx = x + lx * ca - ly * sa;
            float ry = y + lx * sa + ly * ca;
            if (i == 0) TMP_PATH.moveTo(rx, ry);
            else TMP_PATH.lineTo(rx, ry);
        }
        TMP_PATH.close();
        P.setStyle(Paint.Style.FILL);
        P.setColor(fill);
        c.drawPath(TMP_PATH, P);
        if (edge != 0) {
            P.setStyle(Paint.Style.STROKE);
            P.setStrokeWidth(Math.max(1.5f, h * 0.03f));
            P.setColor(edge);
            c.drawPath(TMP_PATH, P);
            P.setStyle(Paint.Style.FILL);
        }
    }

    /** Radial cooldown sweep (dark overlay portion). frac: 0 ready .. 1 full cd. */
    public static void cooldownSweep(Canvas c, float x, float y, float r, float frac, int color) {
        if (frac <= 0f) return;
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        TMP.set(x - r, y - r, x + r, y + r);
        c.drawArc(TMP, -90, 360 * frac, true, P);
    }

    public static String fmt(int v) {
        if (v >= 1000000) return (v / 1000000) + "." + ((v / 100000) % 10) + "M";
        if (v >= 10000) return (v / 1000) + "k";
        return String.valueOf(v);
    }
}
