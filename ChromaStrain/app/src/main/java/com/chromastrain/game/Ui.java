package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;

/** Canvas-drawn UI widgets: buttons, panels, procedural glyph icons. */
public final class Ui {

    private Ui() { }

    /** Rectangular tap button. Call hit() with polled events. */
    public static class Btn {
        public float x, y, w, h;         // center + size
        public String label = "";
        public int color = Palette.PANEL;
        public int edge = Palette.PANEL_EDGE;
        public int textColor = Palette.INK;
        public float textSize = 26f;
        public boolean enabled = true;
        public float pressT;              // press animation

        public Btn set(float cx, float cy, float width, float height) {
            x = cx;
            y = cy;
            w = width;
            h = height;
            return this;
        }

        public boolean contains(float px, float py) {
            return px >= x - w / 2 && px <= x + w / 2 && py >= y - h / 2 && py <= y + h / 2;
        }

        /** Consumes a tap-down within bounds. Fires for disabled buttons too —
         *  callers check {@code enabled} to give deny feedback. */
        public boolean tapped(ArrayList<Input.Ev> events) {
            for (int i = 0; i < events.size(); i++) {
                Input.Ev e = events.get(i);
                if (e.type == 0 && contains(e.x, e.y)) {
                    if (enabled) pressT = 1f;
                    return true;
                }
            }
            return false;
        }

        public void update(float dt) {
            if (pressT > 0) pressT = Math.max(0, pressT - dt * 5f);
        }

        public void draw(Canvas c) {
            float k = 1f - pressT * 0.06f;
            float hw = w / 2 * k, hh = h / 2 * k;
            int fill = enabled ? color : Palette.withAlpha(color, 90);
            G.rr(c, x - hw, y - hh, x + hw, y + hh, hh * 0.45f, fill);
            G.rrStroke(c, x - hw, y - hh, x + hw, y + hh, hh * 0.45f, 2.5f,
                    enabled ? edge : Palette.withAlpha(edge, 70));
            if (label.length() > 0) {
                G.text(c, label, x, y + textSize * 0.35f, textSize,
                        enabled ? textColor : Palette.withAlpha(textColor, 110),
                        Paint.Align.CENTER, G.FONT_BOLD);
            }
        }
    }

    public static void panel(Canvas c, float l, float t, float r, float b) {
        G.rr(c, l, t, r, b, 18, Palette.PANEL);
        G.rrStroke(c, l, t, r, b, 18, 2f, Palette.PANEL_EDGE);
    }

    // -------------------------------------------------------- glyph icons
    // Small procedural icons so the whole art direction stays in one system.

    private static final Path PATH = new Path();

    /** kind: 0 fist(melee) 1 bolt(skill) 2 flask(gadget) 3 syringe(dose) 4 pause 5 play
     *  6 crystal 7 book 8 lab 9 up-arrow 10 gear 11 sound 12 haptic 13 map 14 lock */
    public static void icon(Canvas c, int kind, float x, float y, float s, int color) {
        Paint p = G.P;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.11f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);
        PATH.reset();
        switch (kind) {
            case 0: { // crossed blade slash
                c.drawLine(x - s * 0.38f, y + s * 0.38f, x + s * 0.38f, y - s * 0.38f, p);
                c.drawLine(x - s * 0.38f, y - s * 0.38f, x + s * 0.05f, y + 0.05f * s, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(x + s * 0.3f, y + s * 0.3f, s * 0.09f, p);
                break;
            }
            case 1: { // lightning bolt
                PATH.moveTo(x + s * 0.12f, y - s * 0.45f);
                PATH.lineTo(x - s * 0.25f, y + s * 0.08f);
                PATH.lineTo(x + s * 0.02f, y + s * 0.08f);
                PATH.lineTo(x - s * 0.12f, y + s * 0.45f);
                PATH.lineTo(x + s * 0.28f, y - s * 0.10f);
                PATH.lineTo(x + s * 0.02f, y - s * 0.10f);
                PATH.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(PATH, p);
                break;
            }
            case 2: { // flask
                PATH.moveTo(x - s * 0.12f, y - s * 0.42f);
                PATH.lineTo(x + s * 0.12f, y - s * 0.42f);
                PATH.moveTo(x - s * 0.07f, y - s * 0.40f);
                PATH.lineTo(x - s * 0.07f, y - s * 0.12f);
                PATH.lineTo(x - s * 0.32f, y + s * 0.30f);
                PATH.quadTo(x, y + s * 0.55f, x + s * 0.32f, y + s * 0.30f);
                PATH.lineTo(x + s * 0.07f, y - s * 0.12f);
                PATH.lineTo(x + s * 0.07f, y - s * 0.40f);
                c.drawPath(PATH, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(x, y + s * 0.26f, s * 0.10f, p);
                break;
            }
            case 3: { // syringe (dose)
                c.save();
                c.rotate(-45, x, y);
                p.setStyle(Paint.Style.STROKE);
                c.drawRect(x - s * 0.10f, y - s * 0.30f, x + s * 0.10f, y + s * 0.18f, p);
                c.drawLine(x, y + s * 0.18f, x, y + s * 0.45f, p);
                c.drawLine(x - s * 0.18f, y - s * 0.30f, x + s * 0.18f, y - s * 0.30f, p);
                c.restore();
                break;
            }
            case 4: { // pause
                c.drawLine(x - s * 0.16f, y - s * 0.3f, x - s * 0.16f, y + s * 0.3f, p);
                c.drawLine(x + s * 0.16f, y - s * 0.3f, x + s * 0.16f, y + s * 0.3f, p);
                break;
            }
            case 5: { // play
                PATH.moveTo(x - s * 0.22f, y - s * 0.32f);
                PATH.lineTo(x + s * 0.34f, y);
                PATH.lineTo(x - s * 0.22f, y + s * 0.32f);
                PATH.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(PATH, p);
                break;
            }
            case 6: { // crystal shard
                G.shard(c, x, y, s * 0.7f, s * 0.95f, 0.35f, Palette.withAlpha(color, 70), color);
                break;
            }
            case 7: { // book (codex)
                c.drawLine(x, y - s * 0.34f, x, y + s * 0.34f, p);
                PATH.moveTo(x, y - s * 0.34f);
                PATH.quadTo(x - s * 0.30f, y - s * 0.44f, x - s * 0.42f, y - s * 0.30f);
                PATH.lineTo(x - s * 0.42f, y + s * 0.34f);
                PATH.quadTo(x - s * 0.24f, y + s * 0.24f, x, y + s * 0.34f);
                PATH.quadTo(x + s * 0.24f, y + s * 0.24f, x + s * 0.42f, y + s * 0.34f);
                PATH.lineTo(x + s * 0.42f, y - s * 0.30f);
                PATH.quadTo(x + s * 0.30f, y - s * 0.44f, x, y - s * 0.34f);
                c.drawPath(PATH, p);
                break;
            }
            case 8: { // lab beaker with bubbles
                PATH.moveTo(x - s * 0.2f, y - s * 0.42f);
                PATH.lineTo(x - s * 0.2f, y + s * 0.1f);
                PATH.quadTo(x - s * 0.22f, y + s * 0.42f, x, y + s * 0.42f);
                PATH.quadTo(x + s * 0.22f, y + s * 0.42f, x + s * 0.2f, y + s * 0.1f);
                PATH.lineTo(x + s * 0.2f, y - s * 0.42f);
                c.drawPath(PATH, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(x - s * 0.05f, y + s * 0.2f, s * 0.06f, p);
                c.drawCircle(x + s * 0.09f, y + s * 0.05f, s * 0.045f, p);
                break;
            }
            case 9: { // up arrow
                PATH.moveTo(x, y - s * 0.4f);
                PATH.lineTo(x + s * 0.3f, y + s * 0.05f);
                PATH.lineTo(x + s * 0.12f, y + s * 0.05f);
                PATH.lineTo(x + s * 0.12f, y + s * 0.4f);
                PATH.lineTo(x - s * 0.12f, y + s * 0.4f);
                PATH.lineTo(x - s * 0.12f, y + s * 0.05f);
                PATH.lineTo(x - s * 0.3f, y + s * 0.05f);
                PATH.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(PATH, p);
                break;
            }
            case 10: { // gear
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    c.drawLine(
                            x + (float) Math.cos(a) * s * 0.26f, y + (float) Math.sin(a) * s * 0.26f,
                            x + (float) Math.cos(a) * s * 0.42f, y + (float) Math.sin(a) * s * 0.42f, p);
                }
                c.drawCircle(x, y, s * 0.26f, p);
                break;
            }
            case 11: { // sound
                PATH.moveTo(x - s * 0.35f, y - s * 0.14f);
                PATH.lineTo(x - s * 0.15f, y - s * 0.14f);
                PATH.lineTo(x + s * 0.05f, y - s * 0.32f);
                PATH.lineTo(x + s * 0.05f, y + s * 0.32f);
                PATH.lineTo(x - s * 0.15f, y + s * 0.14f);
                PATH.lineTo(x - s * 0.35f, y + s * 0.14f);
                PATH.close();
                p.setStyle(Paint.Style.FILL);
                c.drawPath(PATH, p);
                p.setStyle(Paint.Style.STROKE);
                c.drawArc(x - s * 0.1f, y - s * 0.3f, x + s * 0.5f, y + s * 0.3f, -50, 100, false, p);
                break;
            }
            case 12: { // haptic waves
                c.drawCircle(x, y, s * 0.10f, p);
                c.drawArc(x - s * 0.3f, y - s * 0.3f, x + s * 0.3f, y + s * 0.3f, -55, 110, false, p);
                c.drawArc(x - s * 0.3f, y - s * 0.3f, x + s * 0.3f, y + s * 0.3f, 125, 110, false, p);
                break;
            }
            case 13: { // map pin
                c.drawCircle(x, y - s * 0.1f, s * 0.22f, p);
                PATH.moveTo(x - s * 0.16f, y + s * 0.04f);
                PATH.lineTo(x, y + s * 0.42f);
                PATH.lineTo(x + s * 0.16f, y + s * 0.04f);
                c.drawPath(PATH, p);
                break;
            }
            case 14: { // lock
                c.drawRect(x - s * 0.26f, y - s * 0.05f, x + s * 0.26f, y + s * 0.38f, p);
                c.drawArc(x - s * 0.17f, y - s * 0.38f, x + s * 0.17f, y + s * 0.05f, 180, 180, false, p);
                break;
            }
            default:
                break;
        }
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.BUTT);
    }
}
