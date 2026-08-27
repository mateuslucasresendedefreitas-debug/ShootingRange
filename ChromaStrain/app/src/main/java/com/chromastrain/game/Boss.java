package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * The nine operation bosses. One class, scripted per opId, each carrying the
 * signature mechanic described in the codex's Field Deployments section.
 */
public class Boss extends Enemy {

    public final int opId;
    public int phase;               // 0,1 (phase 2 at threshold)
    public float stateT;            // generic state timer
    public int state;               // per-boss meaning
    public float atkT;
    public float enrage;            // varrak/echo-byte ramp
    public float openT;             // varrak guard-open window
    public float vulnT;             // echo-byte sync window / nuka exhaust
    public float tellX, tellY, tellT, tellR;   // circular telegraph
    public float ringT;             // matron ring cycle
    public float gapA;              // ring gap angle
    public float summonAt1 = 0.66f, summonAt2 = 0.33f;
    public boolean summoned1, summoned2;
    public float nextSplit = 0.75f;   // priestess re-split threshold
    public float dashDx, dashDy, dashT;
    public float idleHeal;          // clothwalker
    public float sweepA, sweepT;    // scion beam sweep

    public Boss(int opId, float x, float y) {
        this.opId = opId;
        int faction = Ops.faction(opId);
        int slot = Ops.slot(opId);
        init(HUSK, x, y, 1f, 1f, faction, false);
        r = slot == Ops.SLOT_HUNT ? 74 : 62;
        speed = 150;
        float[] hpBySlot = {13000, 21000, 27000};
        hp = maxHp = hpBySlot[slot];
        contactDmg = 130 * Ops.TIER_DMG[slot];
        shotDmg = 95 * Ops.TIER_DMG[slot];
        atkCd = 2f;
        spawnT = 1.2f;
    }

    @Override
    public boolean isBoss() {
        return true;
    }

    @Override
    public float damage(World w, float amount, boolean crit) {
        float mul = 1f;
        switch (opId) {
            case 0: // Varrak guards vs ranged unless opened
                if (openT <= 0 && !w.lastHitWasMelee) mul = 0.10f;
                break;
            case 2: // Nuka takes +50% while exhausted
                if (vulnT > 0) mul = 1.5f;
                break;
            case 5: // Clothwalker only solid while player moves
                if (w.playerIdleT > 1f) mul = 0.0f;
                break;
            case 6: // Echo-Byte sync window
                if (vulnT > 0) mul = 2f;
                break;
            case 7: // Queen shielded while mature virus zones exist
                if (w.matureVirusCount() > 0) mul = 0.55f;
                break;
            default:
                break;
        }
        return super.damage(w, amount * mul, crit);
    }

    @Override
    public void update(World w, float dt) {
        updateStatuses(w, dt);
        if (!alive) return;
        if (spawnT > 0) return;
        if (frozenT > 0 || staggerT > 0) {
            return;
        }
        if (phase == 0 && hp / maxHp < 0.5f) {
            phase = 1;
            w.game.sfx.play("boss_roar", 1f, 0.9f);
            w.fx.burst(x, y, 30, 380, 0.8f, 10, tint, 2);
            w.cam.shake(14f);
            if (opId == 3) { // Mirrored Realm: thorns join
                w.spawnMirrorThorns(this, 2);
                w.showBanner("MIRRORED REALM", Palette.GREEN);
            }
        }
        // shared summons for raid bosses
        int slot = Ops.slot(opId);
        if (slot == Ops.SLOT_RAID) {
            float f = hp / maxHp;
            if (!summoned1 && f < summonAt1) {
                summoned1 = true;
                w.summonAdds(3);
            }
            if (!summoned2 && f < summonAt2) {
                summoned2 = true;
                w.summonAdds(3);
            }
        }

        Player p = w.player;
        float distP = G.dist(x, y, p.x, p.y);
        float ang = G.angleTo(x, y, p.x, p.y);
        atkT -= dt;
        if (openT > 0) openT -= dt;
        if (vulnT > 0) vulnT -= dt;

        switch (opId) {
            case 0: updateVarrak(w, dt, p, distP, ang); break;
            case 1: updateMatron(w, dt, p, distP, ang); break;
            case 2: updateNuka(w, dt, p, distP, ang); break;
            case 3: updateThyral(w, dt, p, distP, ang); break;
            case 4: updatePriestess(w, dt, p, distP, ang); break;
            case 5: updateClothwalker(w, dt, p, distP, ang); break;
            case 6: updateEchoByte(w, dt, p, distP, ang); break;
            case 7: updateQueen(w, dt, p, distP, ang); break;
            default: updateScion(w, dt, p, distP, ang); break;
        }

        x += vx * dt;
        y += vy * dt;
        w.clampToArena(this);

        // body contact
        if (distP < r + p.r && frozenT <= 0) {
            w.hurtPlayer(contactDmg * w.enemyDmgMul * 0.8f, x, y);
        }
    }

    // -------------------------------------------------------- boss scripts

    private void chase(Player p, float ang, float mul) {
        vx = (float) Math.cos(ang) * speed * mul;
        vy = (float) Math.sin(ang) * speed * mul;
    }

    private void updateVarrak(World w, float dt, Player p, float distP, float ang) {
        enrage = Math.min(0.8f, enrage + dt * 0.012f); // berserker mode
        float dmul = 1f + enrage;
        // guard opens when the player commits to close range
        if (distP < 230) {
            openT = 3f;
        }
        if (dashT > 0) {
            dashT -= dt;
            vx = dashDx * 700;
            vy = dashDy * 700;
            if (dashT <= 0) {
                w.shockRing(x, y, 60, 300, 90 * w.enemyDmgMul * dmul);
                w.cam.shake(10f);
            }
            return;
        }
        if (tellT > 0) {
            tellT -= dt;
            vx = vy = 0;
            if (tellT <= 0) {
                dashT = 0.4f;
                float a = G.angleTo(x, y, tellX, tellY);
                dashDx = (float) Math.cos(a);
                dashDy = (float) Math.sin(a);
            }
            return;
        }
        chase(p, ang, 0.9f + enrage * 0.4f);
        if (atkT <= 0) {
            if (distP < 260) {
                // sweep
                atkT = 2.2f - enrage;
                w.bossMeleeSweep(this, ang, 260, 2.6f, 120 * w.enemyDmgMul * dmul);
            } else if (distP > 420) {
                // leap slam telegraph at player pos
                atkT = 3.4f - enrage;
                tellX = p.x;
                tellY = p.y;
                tellR = 180;
                tellT = 0.8f;
            }
        }
        if (phase == 1 && G.rnd() < dt * 0.25f) {
            w.bossMeleeSweep(this, ang + G.rnd(-1f, 1f), 300, 6.28f, 60 * w.enemyDmgMul * dmul);
        }
    }

    private void updateMatron(World w, float dt, Player p, float distP, float ang) {
        // slow stalk
        chase(p, ang, distP > 300 ? 0.55f : 0.2f);
        ringT += dt;
        float interval = phase == 0 ? 7f : 5f;
        if (ringT > interval) {
            ringT = 0;
            gapA = G.rnd(0, 6.283f);
            state = 3; // rings pending
            stateT = 0;
            w.game.sfx.play("boss_roar", 0.9f, 1.1f);
        }
        if (state > 0) {
            stateT += dt;
            if (stateT > 0.55f) {
                stateT = 0;
                state--;
                w.echoRing(x, y, gapA, 0.9f, 110 * w.enemyDmgMul);
            }
        }
        if (atkT <= 0 && distP < 300) {
            atkT = 3f;
            w.bossMeleeSweep(this, ang, 280, 2.2f, 130 * w.enemyDmgMul);
            w.cam.shake(8f);
        }
    }

    private void updateNuka(World w, float dt, Player p, float distP, float ang) {
        stateT -= dt;
        if (state == 0) { // init
            state = 1;
            stateT = 10f;
        }
        if (state == 1) { // RAMPAGE
            vulnT = 0;
            if (dashT > 0) {
                dashT -= dt;
                vx = dashDx * 780;
                vy = dashDy * 780;
                if (dashT <= 0) {
                    w.shockRing(x, y, 70, phase == 1 ? 520 : 340, 100 * w.enemyDmgMul);
                    w.cam.shake(12f);
                    w.game.haptic(30, 140);
                }
            } else {
                chase(p, ang, 1.5f);
                if (atkT <= 0) {
                    atkT = 2.0f;
                    dashT = 0.5f;
                    dashDx = (float) Math.cos(ang);
                    dashDy = (float) Math.sin(ang);
                    w.game.sfx.playVar("dash", 0.9f);
                }
            }
            if (stateT <= 0) {
                state = 2;
                stateT = 6f;
                vulnT = 6f;
                vx = vy = 0;
                w.game.sfx.play("boss_roar", 1f, 0.6f);
                w.showBanner("NUKA COLLAPSES — STRIKE NOW", Palette.GOLD);
            }
        } else { // EXHAUSTED
            vx *= 0.8f;
            vy *= 0.8f;
            vulnT = stateT;
            if (stateT <= 0) {
                state = 1;
                stateT = 10f;
                w.game.sfx.play("boss_roar", 1f, 1.2f);
                w.showBanner("RAMPAGE", Palette.DANGER);
            }
        }
    }

    private void updateThyral(World w, float dt, Player p, float distP, float ang) {
        stateT -= dt;
        if (phase == 1 && state != 9 && stateT <= 0) {
            // Mirrored Realm: teleport to the player's mirror across arena center
            state = 9;
            stateT = 0.7f;
            tellX = w.arenaW - p.x;
            tellY = w.arenaH - p.y;
            tellR = 200;
            tellT = 0.7f;
            return;
        }
        if (state == 9) {
            tellT -= dt;
            if (stateT <= 0) {
                x = tellX;
                y = tellY;
                state = 0;
                stateT = G.rnd(4f, 6f);
                w.fx.burst(x, y, 18, 300, 0.5f, 8, tint, 2);
                w.shockRing(x, y, 40, 260, 95 * w.enemyDmgMul);
            }
            return;
        }
        chase(p, ang, distP > 380 ? 0.8f : -0.5f);
        if (atkT <= 0) {
            atkT = phase == 0 ? 2.4f : 1.7f;
            // ghost bolts: 3-way curved
            for (int i = -1; i <= 1; i++) {
                w.enemyShot(x, y, ang + i * 0.35f, 480, shotDmg * w.enemyDmgMul, tint, false);
            }
            w.game.sfx.playVar("shot_green", 0.4f);
        }
        if (stateT <= 0 && phase == 0) {
            stateT = 7f;
            // blink sideways
            float a2 = ang + (G.rnd() < 0.5f ? 1.9f : -1.9f);
            x += (float) Math.cos(a2) * 260;
            y += (float) Math.sin(a2) * 260;
            w.fx.burst(x, y, 12, 260, 0.4f, 7, tint, 3);
        }
    }

    private void updatePriestess(World w, float dt, Player p, float distP, float ang) {
        if (state == 0) {
            state = 1;
            w.spawnPriestessClones(this, 4);
        }
        // drift in a slow circle around the player
        float orbit = (float) Math.atan2(y - p.y, x - p.x) + dt * 0.35f;
        float rad = Math.max(320, distP * 0.96f);
        float nx = p.x + (float) Math.cos(orbit) * rad;
        float ny = p.y + (float) Math.sin(orbit) * rad;
        vx = (nx - x) / Math.max(dt, 0.0001f) * 0.08f;
        vy = (ny - y) / Math.max(dt, 0.0001f) * 0.08f;
        if (atkT <= 0) {
            atkT = 2.6f;
            for (int i = 0; i < 5; i++) {
                w.enemyShot(x, y, ang + (i - 2) * 0.16f, 460, shotDmg * 0.8f * w.enemyDmgMul, tint, false);
            }
            w.clonesVolley(this);
            w.game.sfx.playVar("shot_green", 0.5f);
        }
        float f = hp / maxHp;
        if (f < nextSplit) {
            nextSplit -= 0.25f;
            w.despawnClones(this);
            // teleport & re-split
            x = G.rnd(300, w.arenaW - 300);
            y = G.rnd(240, w.arenaH - 240);
            w.spawnPriestessClones(this, 4);
            w.showBanner("FIND THE REAL ONE", Palette.GREEN);
        }
    }

    private void updateClothwalker(World w, float dt, Player p, float distP, float ang) {
        boolean playerMoving = w.playerIdleT <= 1f;
        if (!playerMoving) {
            // vanish, drift to the player's back, feed
            idleHeal += dt;
            hp = Math.min(maxHp, hp + maxHp * 0.02f * dt);
            float behind = p.aim + (float) Math.PI;
            float bx = p.x + (float) Math.cos(behind) * 420;
            float by = p.y + (float) Math.sin(behind) * 420;
            x += (bx - x) * dt * 1.5f;
            y += (by - y) * dt * 1.5f;
            vx = vy = 0;
            return;
        }
        if (dashT > 0) {
            dashT -= dt;
            vx = dashDx * 820;
            vy = dashDy * 820;
            if (G.rnd() < dt * 30) {
                w.fx.spawn(x, y, 0, 0, 0.3f, 8, Palette.withAlpha(tint, 90), 3);
            }
            return;
        }
        chase(p, ang, 1.15f);
        if (atkT <= 0) {
            if (distP < 500 && G.rnd() < 0.6f) {
                atkT = phase == 0 ? 2.2f : 1.5f;
                dashT = 0.45f;
                float a = ang + G.rnd(-0.2f, 0.2f);
                dashDx = (float) Math.cos(a);
                dashDy = (float) Math.sin(a);
                w.game.sfx.playVar("dash", 0.8f);
            } else {
                atkT = 2.4f;
                for (int i = 0; i < 4; i++) {
                    w.enemyShot(x, y, ang + (i - 1.5f) * 0.22f, 520, shotDmg * 0.7f * w.enemyDmgMul, tint, false);
                }
            }
        }
    }

    private void updateEchoByte(World w, float dt, Player p, float distP, float ang) {
        enrage += dt * 0.005f; // +2% every 4 seconds, forever
        float dmul = 1f + enrage;
        if (dashT > 0) {
            dashT -= dt;
            vx = dashDx * (900 * (1 + enrage * 0.5f));
            vy = dashDy * (900 * (1 + enrage * 0.5f));
            w.dataTrail(x, y, 30 * w.enemyDmgMul * dmul);
            if (dashT <= 0) {
                vulnT = 1.6f; // sync window
                if (phase == 1 && state == 0) {
                    state = 1;
                    atkT = 0.35f;
                }
            }
            return;
        }
        if (state == 1 && atkT <= 0) {
            state = 0;
            dashT = 0.5f;
            float a = G.angleTo(x, y, p.x, p.y);
            dashDx = (float) Math.cos(a);
            dashDy = (float) Math.sin(a);
            return;
        }
        chase(p, ang, 0.8f + enrage * 0.4f);
        if (atkT <= 0) {
            if (distP > 260) {
                atkT = 2.6f - Math.min(1.2f, enrage);
                dashT = 0.55f;
                dashDx = (float) Math.cos(ang);
                dashDy = (float) Math.sin(ang);
                w.game.sfx.playVar("dash", 1f);
            } else {
                atkT = 1.6f;
                for (int i = 0; i < 6; i++) {
                    w.enemyShot(x, y, ang + (i - 2.5f) * 0.12f, 560, shotDmg * 0.6f * w.enemyDmgMul * dmul, tint, false);
                }
            }
        }
    }

    private void updateQueen(World w, float dt, Player p, float distP, float ang) {
        stateT -= dt;
        if (stateT <= 0) {
            stateT = phase == 0 ? 9f : 6.5f;
            w.addVirusZone();
            w.game.sfx.play("gadget", 0.8f, 0.8f);
        }
        chase(p, ang, distP > 420 ? 0.5f : -0.35f);
        if (atkT <= 0) {
            atkT = phase == 0 ? 2.0f : 1.4f;
            // spiral volley
            float base = w.time * 2.2f;
            for (int i = 0; i < 8; i++) {
                w.enemyShot(x, y, base + i * 0.785f, 380, shotDmg * 0.65f * w.enemyDmgMul, tint, false);
            }
            w.game.sfx.playVar("shot_blue", 0.5f);
        }
        // heal from mature zones
        int mature = w.matureVirusCount();
        if (mature > 0) {
            hp = Math.min(maxHp, hp + maxHp * 0.004f * mature * dt);
        }
    }

    private void updateScion(World w, float dt, Player p, float distP, float ang) {
        stateT -= dt;
        if (stateT <= 0) {
            stateT = phase == 0 ? 8f : 6f;
            w.spawnEchoTurret(p.x, p.y);
            w.showBanner("ECHO DEPLOYED — DODGE YOUR OWN GUN", Palette.BLUE);
        }
        // hex beam sweep
        sweepT -= dt;
        if (sweepT <= 0) {
            sweepT = phase == 0 ? 5f : 3.6f;
            sweepA = G.rnd(0, 6.283f);
            state = 1;
            atkT = 1.0f; // telegraph
        }
        if (state == 1) {
            // atkT already ticks in the shared update — a second decrement here
            // would halve the telegraph window
            vx *= 0.9f;
            vy *= 0.9f;
            if (atkT <= 0) {
                state = 2;
                windup = 1.2f; // beam active duration reuse
            }
        } else if (state == 2) {
            windup -= dt;
            sweepA += dt * 1.5f;
            w.beamDamage(this, sweepA, 900, 95 * w.enemyDmgMul);
            if (windup <= 0) state = 0;
        } else {
            chase(p, ang, distP > 380 ? 0.65f : -0.3f);
        }
        if (atkT <= 0 && state == 0) {
            atkT = 2.2f;
            for (int i = 0; i < 3; i++) {
                w.enemyShot(x, y, ang + (i - 1) * 0.3f, 500, shotDmg * 0.8f * w.enemyDmgMul, tint, true);
            }
        }
    }

    // ------------------------------------------------------------- drawing

    @Override
    public void draw(Canvas c, World w) {
        int alpha = 255;
        if (spawnT > 0) alpha = (int) (255 * (1f - spawnT / 1.2f));
        if (opId == 5) { // clothwalker visibility
            alpha = w.playerIdleT > 1f ? 26 : (int) (alpha * 0.9f);
        }
        float pulse = 1f + (float) Math.sin(w.time * 2.4f) * 0.05f;
        int body = Palette.mix(tintDark, tint, hitFlash > 0 ? 1f : 0.35f);

        boolean weak = vulnT > 0 || openT > 0;
        if (weak) {
            G.glow(c, x, y, r * 3.2f, Palette.withAlpha(Palette.GOLD, 70));
        } else {
            G.glow(c, x, y, r * 2.6f, Palette.withAlpha(tint, alpha / 5));
        }

        // cluster body: three shards
        G.shard(c, x - r * 0.5f, y + r * 0.15f, r * 1.1f, r * 1.7f, -0.5f,
                Palette.withAlpha(body, alpha), Palette.withAlpha(tint, alpha));
        G.shard(c, x + r * 0.5f, y + r * 0.2f, r * 1.05f, r * 1.6f, 0.5f,
                Palette.withAlpha(body, alpha), Palette.withAlpha(tint, alpha));
        G.shard(c, x, y - r * 0.1f, r * 1.3f, r * 2.2f * pulse, 0f,
                Palette.withAlpha(Palette.mix(body, 0xFFFFFFFF, 0.15f), alpha),
                Palette.withAlpha(tint, alpha));
        G.circle(c, x, y - r * 0.15f, r * 0.30f,
                Palette.withAlpha(0xFFFFFFFF, weak ? 255 : (int) (alpha * 0.75f)));

        // telegraph circle
        if (tellT > 0) {
            G.ring(c, tellX, tellY, tellR * (1.2f - tellT), 4f, Palette.withAlpha(Palette.DANGER, 180));
            G.circle(c, tellX, tellY, tellR * (1.2f - tellT) * 0.3f, Palette.withAlpha(Palette.DANGER, 40));
        }
        // scion beam
        if (opId == 8 && (state == 1 || state == 2)) {
            G.P.setStyle(Paint.Style.STROKE);
            G.P.setStrokeWidth(state == 1 ? 4f : 22f);
            G.P.setColor(Palette.withAlpha(state == 1 ? Palette.DANGER : Palette.BLUE, state == 1 ? 140 : 200));
            c.drawLine(x, y, x + (float) Math.cos(sweepA) * 900, y + (float) Math.sin(sweepA) * 900, G.P);
            G.P.setStyle(Paint.Style.FILL);
        }
        drawStatusPips(c, alpha);
    }
}
