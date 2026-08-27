package com.chromastrain.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** Surface + game thread with a fixed-step simulation and per-frame render. */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final float STEP = 1f / 60f;

    private final Game game;
    private Thread thread;
    private volatile boolean running;
    private volatile boolean paused;
    private boolean sized;

    public GameView(Context context) {
        super(context);
        game = new Game(context);
        game.init();
        getHolder().addCallback(this);
        setFocusable(true);
    }

    public void onResumeGame() {
        paused = false;
        game.sfx.resume();
    }

    public void onPauseGame() {
        paused = true;
        game.sfx.pause();
        game.save.flush();
    }

    public boolean handleBack() {
        return game.back();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        game.input.onTouch(event);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this, "chroma-game");
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        game.resize(width, height);
        sized = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) { }
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        float acc = 0f;
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1_000_000_000f;
            last = now;
            if (dt > 0.25f) dt = 0.25f;

            if (!paused && sized) {
                acc += dt;
                int steps = 0;
                while (acc >= STEP && steps < 4) {
                    game.update(STEP);
                    acc -= STEP;
                    steps++;
                }
                if (steps == 4) acc = 0;
            }

            if (sized) {
                Canvas c = null;
                SurfaceHolder holder = getHolder();
                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        c = holder.lockHardwareCanvas();
                    } else {
                        c = holder.lockCanvas();
                    }
                    if (c != null) {
                        game.draw(c);
                    }
                } catch (Exception ignored) {
                } finally {
                    if (c != null) {
                        try {
                            holder.unlockCanvasAndPost(c);
                        } catch (Exception ignored) { }
                    }
                }
            }

            long frame = System.nanoTime() - now;
            long targetNs = 16_666_667L;
            if (frame < targetNs) {
                try {
                    Thread.sleep((targetNs - frame) / 1_000_000L);
                } catch (InterruptedException ignored) { }
            }
        }
    }
}
