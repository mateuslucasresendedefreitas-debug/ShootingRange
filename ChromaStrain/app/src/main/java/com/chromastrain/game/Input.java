package com.chromastrain.game;

import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Multitouch input. Runs on the UI thread (events) and is read by the game
 * thread; all access is synchronized on this object. Coordinates are in
 * virtual units (screen pixels / Game.scale).
 */
public class Input {

    public static final int MAX = 10;

    /** Continuous pointer state. */
    public final boolean[] down = new boolean[MAX];
    public final float[] x = new float[MAX];
    public final float[] y = new float[MAX];

    /** Discrete events polled once per frame. */
    public static class Ev {
        public int type; // 0 down, 1 up, 2 move
        public int id;
        public float x, y;
    }

    private final ArrayList<Ev> queue = new ArrayList<Ev>();
    private final ArrayList<Ev> drain = new ArrayList<Ev>();
    private final ArrayList<Ev> pool = new ArrayList<Ev>();

    private float scale = 1f;

    public void setScale(float s) {
        scale = s;
    }

    public synchronized void onTouch(MotionEvent e) {
        int action = e.getActionMasked();
        int idx = e.getActionIndex();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int id = e.getPointerId(idx);
                if (id < MAX) {
                    down[id] = true;
                    x[id] = e.getX(idx) / scale;
                    y[id] = e.getY(idx) / scale;
                    push(0, id, x[id], y[id]);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    if (id < MAX && down[id]) {
                        x[id] = e.getX(i) / scale;
                        y[id] = e.getY(i) / scale;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (action == MotionEvent.ACTION_CANCEL) {
                    for (int id = 0; id < MAX; id++) {
                        if (down[id]) {
                            down[id] = false;
                            push(1, id, x[id], y[id]);
                        }
                    }
                } else {
                    int id = e.getPointerId(idx);
                    if (id < MAX) {
                        down[id] = false;
                        x[id] = e.getX(idx) / scale;
                        y[id] = e.getY(idx) / scale;
                        push(1, id, x[id], y[id]);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void push(int type, int id, float px, float py) {
        Ev ev = pool.isEmpty() ? new Ev() : pool.remove(pool.size() - 1);
        ev.type = type;
        ev.id = id;
        ev.x = px;
        ev.y = py;
        queue.add(ev);
    }

    /** Game thread: returns this frame's events (valid until next poll). */
    public synchronized ArrayList<Ev> poll() {
        pool.addAll(drain);
        drain.clear();
        drain.addAll(queue);
        queue.clear();
        return drain;
    }

    public synchronized void snapshot(boolean[] outDown, float[] outX, float[] outY) {
        System.arraycopy(down, 0, outDown, 0, MAX);
        System.arraycopy(x, 0, outX, 0, MAX);
        System.arraycopy(y, 0, outY, 0, MAX);
    }

    public synchronized void reset() {
        for (int i = 0; i < MAX; i++) down[i] = false;
        queue.clear();
    }

    // ------------------------------------------------------------ virtual stick

    /** Dynamic virtual joystick: anchors where the finger lands inside its zone. */
    public static class Stick {
        public int pointerId = -1;
        public float baseX, baseY;   // anchor
        public float dx, dy;         // normalized -1..1
        public float mag;            // 0..1
        public final float radius;

        public Stick(float radius) {
            this.radius = radius;
        }

        public boolean active() {
            return pointerId >= 0;
        }

        public void grab(int id, float px, float py) {
            pointerId = id;
            baseX = px;
            baseY = py;
            dx = dy = mag = 0;
        }

        public void release() {
            pointerId = -1;
            dx = dy = mag = 0;
        }

        public void track(boolean[] downArr, float[] xArr, float[] yArr) {
            if (pointerId < 0) return;
            if (!downArr[pointerId]) {
                release();
                return;
            }
            float ox = xArr[pointerId] - baseX;
            float oy = yArr[pointerId] - baseY;
            float l = G.len(ox, oy);
            if (l > radius) {
                // drag the anchor along so the stick "follows" long swipes
                float excess = l - radius;
                baseX += ox / l * excess;
                baseY += oy / l * excess;
                ox = xArr[pointerId] - baseX;
                oy = yArr[pointerId] - baseY;
                l = radius;
            }
            mag = G.clamp(l / radius, 0f, 1f);
            if (l > 0.0001f) {
                dx = ox / radius;
                dy = oy / radius;
            } else {
                dx = dy = 0;
            }
        }
    }
}
