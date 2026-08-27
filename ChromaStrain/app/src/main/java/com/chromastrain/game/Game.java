package com.chromastrain.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.ArrayList;

/**
 * Central game object: virtual resolution, screen stack, services.
 * Virtual space: height is always 720 units; width depends on aspect.
 */
public class Game {

    public static final float VH = 720f;

    public final Context ctx;
    public final Input input = new Input();
    public final Sfx sfx;
    public final Save save;

    public float w = 1280, h = 720;   // virtual size
    public float scale = 1f;          // pixels per unit
    public float time;                // global clock (seconds)

    private Screen screen;
    private Screen nextScreen;
    private float fade;               // 1 = black
    private boolean fadingIn;

    private Vibrator vibrator;
    private final boolean[] tDown = new boolean[Input.MAX];
    private final float[] tX = new float[Input.MAX];
    private final float[] tY = new float[Input.MAX];
    public ArrayList<Input.Ev> events = new ArrayList<Input.Ev>();

    private Paint bgPaint;

    public Game(Context ctx) {
        this.ctx = ctx;
        sfx = new Sfx(ctx);
        save = new Save(ctx);
        sfx.sfxOn = save.sfx;
        sfx.musicOn = save.music;
        vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void init() {
        sfx.load();
        screen = new TitleScreen(this);
        screen.enter();
        fade = 1f;
        fadingIn = true;
    }

    public void resize(int pxW, int pxH) {
        scale = pxH / VH;
        w = pxW / scale;
        h = VH;
        input.setScale(scale);
        bgPaint = new Paint();
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                Palette.VOID_TOP, Palette.VOID_BOT, Shader.TileMode.CLAMP));
    }

    public void switchTo(Screen s) {
        nextScreen = s;
        fadingIn = false;
    }

    public void update(float dt) {
        time += dt;
        events = input.poll();
        input.snapshot(tDown, tX, tY);

        if (nextScreen != null) {
            fade += dt * 4f;
            if (fade >= 1f) {
                fade = 1f;
                if (screen != null) screen.exit();
                screen = nextScreen;
                screen.enter();
                nextScreen = null;
                fadingIn = true;
            }
        } else if (fadingIn && fade > 0f) {
            fade -= dt * 3f;
            if (fade <= 0f) {
                fade = 0f;
                fadingIn = false;
            }
        }

        if (screen != null && nextScreen == null) {
            screen.update(dt);
        }
    }

    public void draw(Canvas c) {
        c.save();
        c.scale(scale, scale);
        if (bgPaint != null) {
            c.drawRect(0, 0, w, h, bgPaint);
        }
        if (screen != null) {
            screen.draw(c);
        }
        if (fade > 0.002f) {
            G.P.setStyle(Paint.Style.FILL);
            G.P.setColor(Palette.withAlpha(0xFF000000, (int) (255 * G.clamp(fade, 0, 1))));
            c.drawRect(0, 0, w, h, G.P);
        }
        c.restore();
    }

    public boolean back() {
        return screen != null && screen.back();
    }

    // -------------------------------------------------------------- helpers

    public boolean touchDown(int id) {
        return id >= 0 && id < Input.MAX && tDown[id];
    }

    public float touchX(int id) {
        return tX[id];
    }

    public float touchY(int id) {
        return tY[id];
    }

    public void haptic(int ms, int amp) {
        if (!save.haptics || vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, Math.max(1, Math.min(255, amp))));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) { }
    }

    public void tapFeedback() {
        sfx.play("ui_tap", 0.7f, 1f);
        haptic(12, 60);
    }
}
