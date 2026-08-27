package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/** Gameplay screen: twin-stick controls, ability buttons, HUD, pause. */
public class RunScreen extends Screen {

    private final World world;
    private final int opId;

    private final Input.Stick moveStick = new Input.Stick(70);
    private final Input.Stick aimStick = new Input.Stick(70);
    private final boolean[] tDown = new boolean[Input.MAX];
    private final float[] tX = new float[Input.MAX];
    private final float[] tY = new float[Input.MAX];

    // ability buttons (world-independent UI) — +17% over the original 46 radius
    private float btnR = 54;
    private float primaryR = 62;
    private float primaryX, primaryY;
    private float meleeX, meleeY, skillX, skillY, gadgetX, gadgetY, doseX, doseY;
    private int meleeHoldPointer = -1;
    private int primaryHoldPointer = -1;
    private boolean paused;
    private final Ui.Btn resumeBtn = new Ui.Btn();
    private final Ui.Btn retryBtn = new Ui.Btn();
    private final Ui.Btn abandonBtn = new Ui.Btn();
    private final Ui.Btn sfxBtn = new Ui.Btn();
    private final Ui.Btn musicBtn = new Ui.Btn();
    private final Ui.Btn hapticBtn = new Ui.Btn();
    private final Ui.Btn shakeBtn = new Ui.Btn();
    private float tutorialT = 9f;
    private boolean ended;

    public RunScreen(Game game, int opId) {
        super(game);
        this.opId = opId;
        world = new World(game, opId);
    }

    @Override
    public void enter() {
        game.sfx.music(world.musicFor(false));
        if (!game.save.tutorialSeen) {
            tutorialT = 12f;
        } else {
            tutorialT = 4f;
        }
    }

    private void layout() {
        float m = 24;
        // PRIMARY anchors the bottom-right corner (bigger — it's held constantly);
        // the ability cluster (secondary/skill/gadget/dose) sits to its upper-left.
        primaryX = game.w - m - primaryR;
        primaryY = game.h - m - primaryR;

        float gap = 34;
        float pitch = btnR * 2.55f;
        float clusterX = primaryX - primaryR - gap - btnR;
        meleeX = clusterX;
        meleeY = game.h - m - btnR;
        skillX = clusterX;
        skillY = meleeY - pitch;
        gadgetX = clusterX - pitch;
        gadgetY = meleeY;
        doseX = clusterX - pitch;
        doseY = meleeY - pitch;
    }

    private boolean overButton(float x, float y) {
        return G.dist(x, y, primaryX, primaryY) < primaryR + 16
                || G.dist(x, y, meleeX, meleeY) < btnR + 16
                || G.dist(x, y, skillX, skillY) < btnR + 16
                || G.dist(x, y, gadgetX, gadgetY) < btnR + 16
                || G.dist(x, y, doseX, doseY) < btnR + 16
                || (x > game.w - 90 && y < 90);
    }

    @Override
    public void update(float dt) {
        layout();
        ArrayList<Input.Ev> evs = game.events;

        if (paused) {
            resumeBtn.update(dt);
            retryBtn.update(dt);
            abandonBtn.update(dt);
            if (resumeBtn.tapped(evs)) {
                game.tapFeedback();
                paused = false;
            } else if (retryBtn.tapped(evs)) {
                game.tapFeedback();
                game.switchTo(new RunScreen(game, opId));
            } else if (abandonBtn.tapped(evs)) {
                game.tapFeedback();
                exitToHub(false);
            } else if (sfxBtn.tapped(evs)) {
                game.save.sfx = !game.save.sfx;
                game.sfx.sfxOn = game.save.sfx;
                game.save.flush();
                game.tapFeedback();
            } else if (musicBtn.tapped(evs)) {
                game.save.music = !game.save.music;
                game.sfx.setMusicOn(game.save.music);
                game.save.flush();
                game.tapFeedback();
            } else if (hapticBtn.tapped(evs)) {
                game.save.haptics = !game.save.haptics;
                game.save.flush();
                game.tapFeedback();
            } else if (shakeBtn.tapped(evs)) {
                game.save.shake = !game.save.shake;
                game.save.flush();
                game.tapFeedback();
            }
            return;
        }

        // interpret touches
        for (int i = 0; i < evs.size(); i++) {
            Input.Ev e = evs.get(i);
            if (e.type != 0) continue;
            // pause button (top-right)
            if (e.x > game.w - 90 && e.y < 90) {
                if (world.state != World.STATE_VICTORY && world.state != World.STATE_DEFEAT) {
                    game.tapFeedback();
                    paused = true;
                }
                continue;
            }
            if (world.player.hp > 0) {
                if (G.dist(e.x, e.y, primaryX, primaryY) < primaryR + 16) {
                    primaryHoldPointer = e.id;
                    continue;
                }
                if (G.dist(e.x, e.y, meleeX, meleeY) < btnR + 16) {
                    world.player.meleeDown();
                    meleeHoldPointer = e.id;
                    continue;
                }
                if (G.dist(e.x, e.y, skillX, skillY) < btnR + 16) {
                    world.player.trySkill();
                    continue;
                }
                if (G.dist(e.x, e.y, gadgetX, gadgetY) < btnR + 16) {
                    world.player.tryGadget();
                    continue;
                }
                if (G.dist(e.x, e.y, doseX, doseY) < btnR + 16) {
                    world.player.tryDose();
                    continue;
                }
            }
            if (e.x < game.w * 0.46f && !moveStick.active()) {
                moveStick.grab(e.id, e.x, e.y);
            } else if (e.x >= game.w * 0.46f && !aimStick.active() && !overButton(e.x, e.y)) {
                aimStick.grab(e.id, e.x, e.y);
            }
        }

        game.input.snapshot(tDown, tX, tY);
        moveStick.track(tDown, tX, tY);
        aimStick.track(tDown, tX, tY);

        if (meleeHoldPointer >= 0) {
            if (world.player.hp > 0 && game.touchDown(meleeHoldPointer)) {
                world.player.meleeHeld(dt);
            } else {
                world.player.meleeRelease();
                meleeHoldPointer = -1;
            }
        }
        if (primaryHoldPointer >= 0 && !(world.player.hp > 0 && game.touchDown(primaryHoldPointer))) {
            primaryHoldPointer = -1;
        }

        float mvx = moveStick.dx, mvy = moveStick.dy;
        float mlen = G.len(mvx, mvy);
        if (mlen > 1f) {
            mvx /= mlen;
            mvy /= mlen;
        }
        if (mlen < 0.12f) {
            mvx = mvy = 0;
        }

        boolean alive = world.player.hp > 0;
        world.player.update(dt, alive ? mvx : 0, alive ? mvy : 0, aimStick.dx, aimStick.dy,
                alive && primaryHoldPointer >= 0);
        world.update(dt);

        if (tutorialT > 0) tutorialT -= dt;

        // end transitions
        if (!ended && (world.state == World.STATE_VICTORY || world.state == World.STATE_DEFEAT)
                && world.stateT <= 0) {
            ended = true;
            boolean victory = world.state == World.STATE_VICTORY;
            // capture reward/record facts BEFORE the save is mutated
            int shards = rewardShards(victory);
            int nodes = rewardNodes(victory);
            boolean newBest = victory && world.score > game.save.best[opId];
            boolean nextWasLocked = Ops.slot(opId) < 2 && !game.save.opUnlocked(opId + 1);
            applyRewards(victory);
            game.switchTo(new ResultScreen(game, opId, victory, world.score, world.kills,
                    (int) world.elapsed, shards, nodes, newBest, victory && nextWasLocked));
        }
    }

    private int rewardShards(boolean victory) {
        if (!victory) return world.shardsFound + world.score / 5000;
        float firstMul = game.save.cleared[opId] ? 0.45f : 1f;
        return (int) (Ops.REWARD_SHARDS[world.slot] * firstMul) + world.score / 2500 + world.shardsFound;
    }

    private int rewardNodes(boolean victory) {
        return world.nodesFound + (victory ? 2 + world.slot : 0);
    }

    private void applyRewards(boolean victory) {
        Save s = game.save;
        s.shards += rewardShards(victory);
        s.nodes += rewardNodes(victory);
        s.tutorialSeen = true;
        if (victory) {
            if (world.score > s.best[opId]) s.best[opId] = world.score;
            s.cleared[opId] = true;
        }
        s.flush();
    }

    private void exitToHub(boolean victory) {
        game.switchTo(new HubScreen(game, 0));
    }

    @Override
    public boolean back() {
        if (paused) {
            paused = false;
        } else if (world.state != World.STATE_VICTORY && world.state != World.STATE_DEFEAT) {
            paused = true;
        }
        return true;
    }

    // ================================================================= draw

    @Override
    public void draw(Canvas c) {
        world.draw(c, game.w, game.h);
        drawHud(c);
        if (paused) drawPause(c);
    }

    private void drawHud(Canvas c) {
        Player p = world.player;
        int fcol = Strain.color(p.faction);

        // dose flash vignette
        if (world.doseFlash > 0) {
            G.P.setColor(Palette.withAlpha(Palette.DOSE, (int) (70 * world.doseFlash)));
            c.drawRect(0, 0, game.w, game.h, G.P);
        }
        if (p.hurtFlash > 0) {
            G.P.setColor(Palette.withAlpha(Palette.DANGER, (int) (60 * p.hurtFlash / 0.3f)));
            c.drawRect(0, 0, game.w, game.h, G.P);
        }

        // HP + dose (top-left)
        float bx = 24, by = 24, bw = 300, bh = 22;
        G.rr(c, bx, by, bx + bw, by + bh, 8, Palette.HP_BACK);
        float f = G.clamp(p.hp / p.maxHp, 0, 1);
        G.rr(c, bx, by, bx + bw * f, by + bh, 8, f < 0.3f ? Palette.DANGER : Palette.HP);
        G.textB(c, (int) Math.max(0, p.hp) + " / " + (int) p.maxHp, bx + 8, by + bh - 5, 15, 0xFFFFFFFF);

        float dy = by + bh + 8;
        G.rr(c, bx, dy, bx + bw * 0.7f, dy + 12, 5, Palette.withAlpha(Palette.DOSE, 50));
        G.rr(c, bx, dy, bx + bw * 0.7f * (p.doseMeter / 100f), dy + 12, 5, Palette.DOSE);
        if (p.doseT > 0) {
            G.textB(c, "DOSED " + (int) Math.ceil(p.doseT) + "s", bx + bw * 0.7f + 10, dy + 11, 15, Palette.DOSE);
        } else if (p.withdrawalT > 0) {
            G.textB(c, "WITHDRAWAL", bx + bw * 0.7f + 10, dy + 11, 15, Palette.INK_DIM);
        }

        // status line
        float sy = dy + 30;
        if (p.stealthT > 0) {
            G.textB(c, "CLOAKED", bx, sy, 16, Palette.GREEN);
            sy += 20;
        }
        if (p.dataCharges > 0) {
            G.textB(c, "DATA x" + p.dataCharges, bx, sy, 16, Palette.BLUE);
        }

        // top-center: wave / boss bar / banner
        if (world.state == World.STATE_BOSS || world.state == World.STATE_BOSS_INTRO) {
            if (world.boss != null && world.boss.alive) {
                float w2 = Math.min(560, game.w * 0.5f);
                float cx = game.w / 2;
                G.textCB(c, Ops.BOSS_NAME[opId], cx, 34, 19, Palette.INK);
                G.rr(c, cx - w2 / 2, 44, cx + w2 / 2, 60, 7, Palette.withAlpha(0xFF000000, 140));
                float bf = G.clamp(world.boss.hp / world.boss.maxHp, 0, 1);
                G.rr(c, cx - w2 / 2, 44, cx - w2 / 2 + w2 * bf, 60, 7, Palette.DANGER);
                boolean weak = world.boss.vulnT > 0 || world.boss.openT > 0;
                if (weak) {
                    G.textCB(c, "VULNERABLE", cx, 78, 16, Palette.GOLD);
                }
            }
        } else if (world.state == World.STATE_WAVE || world.state == World.STATE_INTERLUDE) {
            G.textCB(c, "WAVE " + world.wave + " / " + world.totalWaves, game.w / 2, 36, 20, Palette.INK_DIM);
        }

        // banner
        if (world.bannerT > 0) {
            float a = G.clamp(world.bannerT / 0.4f, 0, 1);
            float slide = (1f - G.clamp((2.2f - world.bannerT) / 0.25f, 0, 1)) * 30;
            G.textCB(c, world.bannerText, game.w / 2, game.h * 0.30f - slide, 40,
                    Palette.withAlpha(world.bannerColor, (int) (255 * a)));
        }

        // score (top-right, left of pause)
        G.textR(c, G.fmt(world.score), game.w - 104, 44, 26, Palette.INK);
        if (world.scoreMul100 > 100) {
            G.textR(c, "x" + (world.scoreMul100 / 100f), game.w - 104, 68, 16, Palette.GOLD);
        }

        // pause button
        G.rr(c, game.w - 78, 18, game.w - 22, 74, 14, Palette.PANEL);
        Ui.icon(c, 4, game.w - 50, 46, 30, Palette.INK_DIM);

        // sticks
        if (moveStick.active()) {
            drawStick(c, moveStick, fcol);
        }
        if (aimStick.active()) {
            drawStick(c, aimStick, Palette.INK_DIM);
        }

        // PRIMARY — dedicated hold-to-fire button; the aim stick only steers now
        boolean holding = primaryHoldPointer >= 0;
        G.circle(c, primaryX, primaryY, primaryR, Palette.withAlpha(Palette.PANEL, 220));
        G.ring(c, primaryX, primaryY, primaryR, 3f, Palette.withAlpha(fcol, holding ? 255 : 150));
        Ui.icon(c, 17, primaryX, primaryY, primaryR * 0.95f, Palette.withAlpha(fcol, holding ? 255 : 190));
        if (holding) {
            float pulse = (float) Math.sin(game.time * 14f) * 0.5f + 0.5f;
            G.ring(c, primaryX, primaryY, primaryR + 6 + pulse * 5, 3f,
                    Palette.withAlpha(fcol, (int) (140 + 100 * pulse)));
        }

        // ability buttons — the attack icon reflects what that button actually does now
        int atkIcon = p.faction == Strain.BLUE ? 16 : 0;
        drawAbility(c, meleeX, meleeY, atkIcon, fcol, p.meleeCd, 1f / Strain.MELEE[p.faction][2], -1, true);
        drawAbility(c, skillX, skillY, 1, fcol, p.skillCd, Strain.SKILL_CD[p.faction] * p.cdMul, -1, true);
        drawAbility(c, gadgetX, gadgetY, 2, fcol, p.gadgetCd, Strain.GADGET_CD[p.faction] * p.cdMul, -1, true);
        boolean doseUp = p.doseReady();
        drawAbility(c, doseX, doseY, 3, doseUp ? Palette.DOSE : Palette.INK_DIM,
                0, 1, p.doseMeter / 100f, doseUp);

        // secondary-attack identity cues: red charges, green chains a combo, blue is a plain shot
        if (p.faction == Strain.RED && p.meleeCharging) {
            float pulse = (float) Math.sin(game.time * 10f) * 0.5f + 0.5f;
            G.ring(c, meleeX, meleeY, btnR + 8 + pulse * 4, 3f,
                    Palette.withAlpha(Palette.GOLD, (int) (150 + 100 * pulse)));
        } else if (p.faction == Strain.GREEN) {
            int pip = p.comboIdx % 3;
            float dotY = meleeY - btnR - 16;
            for (int i = 0; i < 3; i++) {
                float dxp = meleeX + (i - 1) * 15;
                G.circle(c, dxp, dotY, 5, i < pip
                        ? Strain.color(p.faction) : Palette.withAlpha(Palette.INK_DIM, 90));
            }
        }

        // tutorial hints
        if (tutorialT > 0 && world.state != World.STATE_VICTORY && world.state != World.STATE_DEFEAT) {
            int a = (int) (200 * G.clamp(tutorialT, 0, 1));
            G.textCB(c, "LEFT — MOVE", game.w * 0.22f, game.h - 40, 18, Palette.withAlpha(Palette.INK_DIM, a));
            G.textCB(c, "RIGHT — AIM", game.w * 0.58f, game.h - 40, 18, Palette.withAlpha(Palette.INK_DIM, a));
            G.textCB(c, "HOLD TO FIRE", primaryX, primaryY - primaryR - 18, 15,
                    Palette.withAlpha(Palette.INK_DIM, a));
            G.textR(c, "SECONDARY / SKILL / GADGET / DOSE", game.w - 24, game.h - btnR * 5.6f, 15,
                    Palette.withAlpha(Palette.INK_DIM, a));
        }

        // interlude countdown
        if (world.state == World.STATE_INTRO) {
            G.textCB(c, Ops.SLOT_TAG[world.slot] + " — " + Ops.MODIFIER[opId],
                    game.w / 2, game.h * 0.62f, 18, Palette.INK_DIM);
        }
    }

    private void drawStick(Canvas c, Input.Stick s, int color) {
        G.ring(c, s.baseX, s.baseY, s.radius, 3f, Palette.withAlpha(color, 70));
        G.circle(c, s.baseX + s.dx * s.radius, s.baseY + s.dy * s.radius, 26,
                Palette.withAlpha(color, 130));
    }

    private void drawAbility(Canvas c, float x, float y, int icon, int color,
                             float cd, float cdMax, float meter, boolean ready) {
        G.circle(c, x, y, btnR, Palette.withAlpha(Palette.PANEL, 220));
        G.ring(c, x, y, btnR, 2.5f, Palette.withAlpha(color, ready ? 220 : 90));
        Ui.icon(c, icon, x, y, btnR * 0.95f, Palette.withAlpha(color, ready ? 255 : 120));
        if (cd > 0 && cdMax > 0) {
            G.cooldownSweep(c, x, y, btnR, G.clamp(cd / cdMax, 0, 1),
                    Palette.withAlpha(0xFF000000, 150));
            G.textCB(c, String.valueOf((int) Math.ceil(cd)), x, y + 7, 22, 0xFFFFFFFF);
        }
        if (meter >= 0) {
            G.P.setStyle(Paint.Style.STROKE);
            G.P.setStrokeWidth(4f);
            G.P.setColor(Palette.DOSE);
            c.drawArc(x - btnR + 3, y - btnR + 3, x + btnR - 3, y + btnR - 3,
                    -90, 360 * G.clamp(meter, 0, 1), false, G.P);
            G.P.setStyle(Paint.Style.FILL);
            if (ready) {
                float pulse = (float) Math.sin(game.time * 6f) * 0.5f + 0.5f;
                G.ring(c, x, y, btnR + 6 + pulse * 4, 2f, Palette.withAlpha(Palette.DOSE, (int) (120 * pulse + 60)));
            }
        }
    }

    private void drawPause(Canvas c) {
        G.P.setColor(Palette.withAlpha(0xFF000000, 170));
        c.drawRect(0, 0, game.w, game.h, G.P);
        float cx = game.w / 2;
        G.textCB(c, "PAUSED", cx, game.h * 0.26f, 44, Palette.INK);
        G.textC(c, Ops.NAME[opId] + " — " + Ops.MODIFIER[opId], cx, game.h * 0.34f, 16, Palette.INK_DIM);

        resumeBtn.set(cx, game.h * 0.48f, 320, 62);
        resumeBtn.label = "RESUME";
        resumeBtn.color = Palette.withAlpha(Strain.color(game.save.faction), 40);
        resumeBtn.edge = Strain.color(game.save.faction);
        resumeBtn.draw(c);

        retryBtn.set(cx, game.h * 0.60f, 320, 62);
        retryBtn.label = "RESTART OP";
        retryBtn.draw(c);

        abandonBtn.set(cx, game.h * 0.72f, 320, 62);
        abandonBtn.label = "ABANDON";
        abandonBtn.textColor = Palette.DANGER;
        abandonBtn.draw(c);

        // quick settings
        float sy = game.h * 0.84f;
        sfxBtn.set(cx - 150, sy, 80, 58);
        sfxBtn.label = "";
        sfxBtn.draw(c);
        Ui.icon(c, 11, cx - 150, sy, 28,
                game.save.sfx ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
        musicBtn.set(cx - 50, sy, 80, 58);
        musicBtn.draw(c);
        G.textCB(c, "♪", cx - 50, sy + 9, 28,
                game.save.music ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
        hapticBtn.set(cx + 50, sy, 80, 58);
        hapticBtn.draw(c);
        Ui.icon(c, 12, cx + 50, sy, 28,
                game.save.haptics ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
        shakeBtn.set(cx + 150, sy, 80, 58);
        shakeBtn.draw(c);
        Ui.icon(c, 15, cx + 150, sy, 28,
                game.save.shake ? Palette.INK : Palette.withAlpha(Palette.INK_DIM, 90));
    }
}
