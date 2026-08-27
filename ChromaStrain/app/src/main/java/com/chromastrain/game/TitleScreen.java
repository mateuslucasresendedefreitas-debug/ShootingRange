package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Title: animated tri-strain crystal, start, quick settings. */
public class TitleScreen extends Screen {

    private final Ui.Btn startBtn = new Ui.Btn();
    private final Ui.Btn sfxBtn = new Ui.Btn();
    private final Ui.Btn musicBtn = new Ui.Btn();
    private final Ui.Btn hapticBtn = new Ui.Btn();
    private final Ui.Btn shakeBtn = new Ui.Btn();
    private float t;

    public TitleScreen(Game game) {
        super(game);
    }

    @Override
    public void enter() {
        game.sfx.music("menu");
    }

    @Override
    public void update(float dt) {
        t += dt;
        startBtn.update(dt);
        if (startBtn.tapped(game.events)) {
            game.tapFeedback();
            game.switchTo(new HubScreen(game, 0));
            return;
        }
        if (sfxBtn.tapped(game.events)) {
            game.save.sfx = !game.save.sfx;
            game.sfx.sfxOn = game.save.sfx;
            game.save.flush();
            game.tapFeedback();
        }
        if (musicBtn.tapped(game.events)) {
            game.save.music = !game.save.music;
            game.sfx.setMusicOn(game.save.music);
            game.save.flush();
            game.tapFeedback();
        }
        if (hapticBtn.tapped(game.events)) {
            game.save.haptics = !game.save.haptics;
            game.save.flush();
            game.tapFeedback();
        }
        if (shakeBtn.tapped(game.events)) {
            game.save.shake = !game.save.shake;
            game.save.flush();
            game.tapFeedback();
        }
    }

    @Override
    public void draw(Canvas c) {
        float cx = game.w / 2;
        float cy = game.h * 0.40f;

        // ambient drifting shards
        for (int i = 0; i < 14; i++) {
            float px = (i * 173.3f + t * (12 + i * 3)) % (game.w + 200) - 100;
            float py = (i * 97.7f) % game.h;
            int col = i % 3 == 0 ? Palette.RED : (i % 3 == 1 ? Palette.GREEN : Palette.BLUE);
            G.shard(c, px, py, 10 + i % 3 * 5, 16 + i % 4 * 6,
                    t * 0.3f + i, Palette.withAlpha(col, 24), 0);
        }

        // central cluster
        float pulse = (float) Math.sin(t * 1.6f) * 0.06f + 1f;
        G.glow(c, cx, cy, 240 * pulse, Palette.withAlpha(Palette.GREEN, 26));
        G.shard(c, cx - 74, cy + 26, 66, 120, -0.42f + (float) Math.sin(t * 0.9f) * 0.03f,
                Palette.mix(Palette.RED_DARK, Palette.RED, 0.5f), Palette.RED);
        G.shard(c, cx + 74, cy + 30, 62, 112, 0.46f + (float) Math.sin(t * 1.1f) * 0.03f,
                Palette.mix(Palette.BLUE_DARK, Palette.BLUE, 0.5f), Palette.BLUE);
        G.shard(c, cx, cy - 12, 74, 158 * pulse, 0f,
                Palette.mix(Palette.GREEN_DARK, Palette.GREEN, 0.5f), Palette.GREEN);
        G.circle(c, cx, cy - 6, 13 + (float) Math.sin(t * 3f) * 2f, 0xFFFFFFFF);

        // title
        G.text(c, "CHROMA", cx, game.h * 0.70f, 84, Palette.INK, Paint.Align.CENTER, G.FONT_BOLD);
        G.text(c, "STRAIN", cx, game.h * 0.70f + 66, 84,
                Palette.mix(Palette.GREEN, Palette.BLUE, (float) (Math.sin(t) * 0.5 + 0.5)),
                Paint.Align.CENTER, G.FONT_BOLD);
        G.textC(c, "a Chromanite codex game", cx, game.h * 0.70f + 96, 17, Palette.INK_DIM);

        startBtn.set(cx, game.h * 0.88f, 340, 66);
        startBtn.label = "DEPLOY";
        startBtn.textSize = 30;
        startBtn.color = Palette.withAlpha(Palette.GREEN, 36);
        startBtn.edge = Palette.GREEN;
        startBtn.draw(c);

        // settings chips (top-right)
        float bx = game.w - 60, by = 52;
        sfxBtn.set(bx, by, 72, 60);
        sfxBtn.label = "";
        sfxBtn.draw(c);
        Ui.icon(c, 11, bx, by, 30, game.save.sfx ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
        musicBtn.set(bx - 84, by, 72, 60);
        musicBtn.draw(c);
        G.textCB(c, "♪", bx - 84, by + 9, 30,
                game.save.music ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
        hapticBtn.set(bx - 168, by, 72, 60);
        hapticBtn.draw(c);
        Ui.icon(c, 12, bx - 168, by, 30,
                game.save.haptics ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
        shakeBtn.set(bx - 252, by, 72, 60);
        shakeBtn.draw(c);
        Ui.icon(c, 15, bx - 252, by, 30,
                game.save.shake ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));

        G.textC(c, "v1.0 — adapted from the Codex: Skills, Gadgets, Weapons & Operations",
                cx, game.h - 14, 13, Palette.withAlpha(Palette.INK_DIM, 140));
    }
}
