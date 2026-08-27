package com.chromastrain.game;

import android.graphics.Canvas;

/** Post-run summary: score breakdown, rewards, unlocks. */
public class ResultScreen extends Screen {

    private final int opId;
    private final boolean victory;
    private final int score, kills, seconds, shards, nodes;
    private final boolean newBest;
    private final Ui.Btn retryBtn = new Ui.Btn();
    private final Ui.Btn hubBtn = new Ui.Btn();
    private final Ui.Btn nextBtn = new Ui.Btn();
    private float t;
    private final boolean unlockedNext;   // newly unlocked THIS run (stat line)
    private final boolean showNext;       // next op is playable (nav button)

    public ResultScreen(Game game, int opId, boolean victory, int score, int kills,
                        int seconds, int shards, int nodes, boolean newBest,
                        boolean unlockedNext) {
        super(game);
        this.opId = opId;
        this.victory = victory;
        this.score = score;
        this.kills = kills;
        this.seconds = seconds;
        this.shards = shards;
        this.nodes = nodes;
        this.newBest = newBest && victory;
        this.unlockedNext = unlockedNext;
        showNext = victory && Ops.slot(opId) < 2 && game.save.opUnlocked(opId + 1);
    }

    @Override
    public void enter() {
        game.sfx.music("menu");
    }

    @Override
    public void update(float dt) {
        t += dt;
        retryBtn.update(dt);
        hubBtn.update(dt);
        nextBtn.update(dt);
        if (retryBtn.tapped(game.events)) {
            game.tapFeedback();
            game.switchTo(new RunScreen(game, opId));
        } else if (hubBtn.tapped(game.events)) {
            game.tapFeedback();
            game.switchTo(new HubScreen(game, 0));
        } else if (showNext && nextBtn.tapped(game.events)) {
            game.tapFeedback();
            game.switchTo(new RunScreen(game, opId + 1));
        }
    }

    @Override
    public boolean back() {
        game.switchTo(new HubScreen(game, 0));
        return true;
    }

    @Override
    public void draw(Canvas c) {
        float cx = game.w / 2;
        int f = Ops.faction(opId);
        int col = victory ? Strain.color(f) : Palette.DANGER;

        float a = G.clamp(t * 2f, 0, 1);
        G.glow(c, cx, game.h * 0.2f, 300, Palette.withAlpha(col, (int) (40 * a)));
        G.textCB(c, victory ? "OPERATION COMPLETE" : "SIGNAL LOST", cx, game.h * 0.17f, 52,
                Palette.withAlpha(col, (int) (255 * a)));
        G.textC(c, Ops.NAME[opId] + "  ·  " + Ops.SLOT_TAG[Ops.slot(opId)], cx, game.h * 0.24f,
                18, Palette.INK_DIM);
        if (newBest) {
            G.textCB(c, "★ NEW BEST", cx, game.h * 0.30f, 22, Palette.GOLD);
        }

        // stats panel
        float py = game.h * 0.36f;
        Ui.panel(c, cx - 330, py, cx + 330, py + 190);
        drawStat(c, cx - 300, py + 46, "SCORE", G.fmt(score), Palette.INK);
        drawStat(c, cx - 300, py + 96, "KILLS", String.valueOf(kills), Palette.INK);
        drawStat(c, cx - 300, py + 146, "TIME", (seconds / 60) + ":" + String.format("%02d", seconds % 60), Palette.INK);
        drawStat(c, cx + 40, py + 46, "SHARDS EARNED", "+" + shards, Palette.SHARD);
        drawStat(c, cx + 40, py + 96, "RAW NODES", "+" + nodes, Palette.DOSE);
        if (unlockedNext) {
            drawStat(c, cx + 40, py + 146, "UNLOCKED", Ops.NAME[opId + 1], Strain.color(f));
        } else if (!victory) {
            drawStat(c, cx + 40, py + 146, "TIP", Ops.BOSS_HINT[opId].length() > 34
                    ? Ops.BOSS_HINT[opId].substring(0, 34) + "…" : Ops.BOSS_HINT[opId], Palette.INK_DIM);
        }

        float by = game.h * 0.87f;
        retryBtn.set(cx - (showNext ? 250 : 130), by, 230, 64);
        retryBtn.label = victory ? "REPLAY" : "RETRY";
        retryBtn.color = Palette.withAlpha(col, 36);
        retryBtn.edge = col;
        retryBtn.draw(c);

        if (showNext) {
            nextBtn.set(cx, by, 230, 64);
            nextBtn.label = "NEXT OP";
            nextBtn.color = Palette.withAlpha(Strain.color(f), 60);
            nextBtn.edge = Strain.color(f);
            nextBtn.draw(c);
        }

        hubBtn.set(cx + (showNext ? 250 : 130), by, 230, 64);
        hubBtn.label = "HANGAR";
        hubBtn.draw(c);
    }

    private void drawStat(Canvas c, float x, float y, String label, String value, int color) {
        G.textB(c, label, x, y - 18, 13, Palette.INK_DIM);
        G.textB(c, value, x, y + 8, 26, color);
    }
}
