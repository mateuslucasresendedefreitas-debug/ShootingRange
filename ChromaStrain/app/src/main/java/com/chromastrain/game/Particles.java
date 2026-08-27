package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Pooled particle system (world space). */
public class Particles {

    private static final int MAX = 480;

    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final float[] life = new float[MAX];
    private final float[] maxLife = new float[MAX];
    private final float[] size = new float[MAX];
    private final int[] color = new int[MAX];
    private final int[] kind = new int[MAX]; // 0 dot, 1 spark(line), 2 shard, 3 smoke ring
    private int head;

    public void spawn(float px, float py, float velX, float velY, float lifetime,
                      float sz, int col, int k) {
        int i = head;
        head = (head + 1) % MAX;
        x[i] = px;
        y[i] = py;
        vx[i] = velX;
        vy[i] = velY;
        life[i] = lifetime;
        maxLife[i] = lifetime;
        size[i] = sz;
        color[i] = col;
        kind[i] = k;
    }

    public void burst(float px, float py, int count, float speed, float lifetime,
                      float sz, int col, int k) {
        for (int i = 0; i < count; i++) {
            float a = G.rnd(0, (float) (Math.PI * 2));
            float s = speed * G.rnd(0.35f, 1f);
            spawn(px, py, (float) Math.cos(a) * s, (float) Math.sin(a) * s,
                    lifetime * G.rnd(0.6f, 1.1f), sz * G.rnd(0.7f, 1.3f), col, k);
        }
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            life[i] -= dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            vx[i] *= (1f - 2.6f * dt);
            vy[i] *= (1f - 2.6f * dt);
        }
    }

    public void draw(Canvas c) {
        Paint p = G.P;
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            float t = life[i] / maxLife[i];
            int a = (int) (255 * t * t);
            int col = Palette.withAlpha(color[i], Math.min(255, a));
            switch (kind[i]) {
                case 1: {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(size[i] * 0.4f);
                    p.setColor(col);
                    c.drawLine(x[i], y[i], x[i] - vx[i] * 0.05f, y[i] - vy[i] * 0.05f, p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                }
                case 2: {
                    G.shard(c, x[i], y[i], size[i], size[i] * 1.5f,
                            (x[i] + y[i]) * 0.01f + t * 4f, col, 0);
                    break;
                }
                case 3: {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(size[i] * 0.25f);
                    p.setColor(Palette.withAlpha(color[i], (int) (a * 0.6f)));
                    c.drawCircle(x[i], y[i], size[i] * (1.6f - t), p);
                    p.setStyle(Paint.Style.FILL);
                    break;
                }
                default: {
                    p.setColor(col);
                    c.drawCircle(x[i], y[i], size[i] * (0.5f + t * 0.5f), p);
                    break;
                }
            }
        }
    }

    public void clear() {
        for (int i = 0; i < MAX; i++) life[i] = 0;
    }
}
