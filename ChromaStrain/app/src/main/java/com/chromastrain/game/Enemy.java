package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Chromanite husks — five archetypes shared by every operation, tinted by the
 * op's strain. Also the base class for bosses (status handling + damage).
 */
public class Enemy {

    public static final int HUSK = 0;      // melee chaser
    public static final int SPITTER = 1;   // ranged
    public static final int BRUTE = 2;     // telegraphed charger
    public static final int VOLATILE = 3;  // fast exploder
    public static final int WARDEN = 4;    // support: shields allies

    public int type;
    public boolean alive = true;
    public boolean elite;
    public float x, y, vx, vy, r = 22;
    public float hp, maxHp;
    public float speed;
    public float contactDmg;
    public float shotDmg;
    public int tint;
    public int tintDark;

    // statuses
    public float burnT, burnDps;
    public int bleedStacks;
    public float bleedT;
    public boolean hemorrhage;
    public float hemoT;
    public int chillStacks;
    public float chillT;
    public float frozenT;
    public float disruptT;
    public float staggerT;
    public float disorientT;
    public float critVulnT;      // after freeze: +crit damage taken
    public float weakenT;        // deals less damage (overcharge wave)

    // ai
    public float atkCd;
    public float windup;         // telegraph timer for brute charge / spitter volley
    public float chargeT;        // active charge time
    public float chargeDx, chargeDy;
    public float wanderA;
    public float shieldHp;       // warden bubble
    public float hitFlash;
    public float spawnT;         // spawn-in animation
    public float phaseSeed;

    public Enemy() { }

    public void init(int type, float x, float y, float hpMul, float dmgMul, int faction, boolean elite) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.elite = elite;
        alive = true;
        vx = vy = 0;
        burnT = 0; burnDps = 0; bleedStacks = 0; bleedT = 0; hemorrhage = false; hemoT = 0;
        chillStacks = 0; chillT = 0; frozenT = 0; disruptT = 0; staggerT = 0; disorientT = 0;
        critVulnT = 0; weakenT = 0; atkCd = G.rnd(0.4f, 1.4f); windup = 0; chargeT = 0;
        shieldHp = 0; hitFlash = 0; spawnT = 0.5f;
        wanderA = G.rnd(0, 6.28f);
        phaseSeed = G.rnd(0, 100f);
        tint = Strain.color(faction);
        tintDark = Strain.colorDark(faction);

        float e = elite ? 2.6f : 1f;
        switch (type) {
            case SPITTER:
                r = 24; speed = 150; hp = 420 * hpMul * e; contactDmg = 60 * dmgMul; shotDmg = 85 * dmgMul;
                break;
            case BRUTE:
                r = 40; speed = 120; hp = 1500 * hpMul * e; contactDmg = 110 * dmgMul; shotDmg = 0;
                break;
            case VOLATILE:
                r = 18; speed = 300; hp = 220 * hpMul * e; contactDmg = 150 * dmgMul; shotDmg = 0;
                break;
            case WARDEN:
                r = 28; speed = 130; hp = 900 * hpMul * e; contactDmg = 70 * dmgMul; shotDmg = 0;
                shieldHp = 500 * hpMul;
                break;
            default: // HUSK
                r = 22; speed = 215; hp = 380 * hpMul * e; contactDmg = 80 * dmgMul; shotDmg = 0;
                break;
        }
        if (elite) r *= 1.35f;
        maxHp = hp;
    }

    public boolean isBoss() {
        return false;
    }

    /** Effective movement multiplier from statuses. */
    public float moveMul(World w) {
        if (frozenT > 0 || staggerT > 0 || spawnT > 0) return 0;
        float m = 1f;
        if (chillStacks > 0) m *= (1f - 0.18f * chillStacks);
        if (disruptT > 0) m *= 0.9f;
        m *= w.enemySpeedMul;
        return m;
    }

    public float damageDealtMul() {
        float m = weakenT > 0 ? 0.9f : 1f;
        return m;
    }

    /** Applies damage; returns actual amount dealt (after modifiers). */
    public float damage(World w, float amount, boolean crit) {
        if (!alive) return 0;
        float mul = 1f;
        if (hemorrhage) mul *= 1.25f;
        if (crit && critVulnT > 0) mul *= 1.2f;
        float a = amount * mul;
        if (shieldHp > 0) {
            shieldHp -= a;
            hitFlash = 0.1f;
            if (shieldHp < 0) {
                a = -shieldHp;
                shieldHp = 0;
            } else {
                return a;
            }
        }
        hp -= a;
        hitFlash = 0.12f;
        if (hp <= 0) {
            alive = false;
            w.onEnemyKilled(this);
        }
        return a;
    }

    public void applyChill(World w, int stacks) {
        chillT = 4f;
        chillStacks += stacks;
        if (chillStacks >= 3 && frozenT <= 0) {
            chillStacks = 0;
            frozenT = isBoss() ? 0.9f : 2f;
            critVulnT = 4f;
            windup = 0;
            chargeT = 0;
            w.game.sfx.playVar("freeze", 0.8f);
            w.fx.burst(x, y, 10, 220, 0.5f, 7, Palette.BLUE, 2);
        }
    }

    public void applyBleed(World w) {
        bleedT = 5f;
        if (bleedStacks < 5) bleedStacks++;
        if (bleedStacks >= 5 && !hemorrhage) {
            hemorrhage = true;
            hemoT = 4f;
            w.fx.burst(x, y, 8, 200, 0.4f, 6, Palette.GREEN, 1);
        }
    }

    public void applyBurn(World w, float dps) {
        burnT = 4f;
        burnDps = Math.max(burnDps, dps);
    }

    protected void updateStatuses(World w, float dt) {
        if (hitFlash > 0) hitFlash -= dt;
        if (spawnT > 0) spawnT -= dt;
        if (burnT > 0) {
            burnT -= dt;
            damage(w, burnDps * dt, false);
            if (G.rnd() < dt * 8) w.fx.spawn(x + G.rnd(-r, r), y + G.rnd(-r, r),
                    G.rnd(-20, 20), -G.rnd(30, 80), 0.4f, 5, Palette.RED, 0);
            if (burnT <= 0) burnDps = 0;
        }
        if (bleedT > 0) {
            bleedT -= dt;
            if (bleedStacks > 0) damage(w, 26 * bleedStacks * dt, false);
            if (bleedT <= 0) bleedStacks = 0;
        }
        if (hemorrhage) {
            hemoT -= dt;
            if (hemoT <= 0) hemorrhage = false;
        }
        if (chillT > 0) {
            chillT -= dt;
            if (chillT <= 0) chillStacks = 0;
        }
        if (frozenT > 0) frozenT -= dt;
        if (disruptT > 0) disruptT -= dt;
        if (staggerT > 0) staggerT -= dt;
        if (disorientT > 0) disorientT -= dt;
        if (critVulnT > 0) critVulnT -= dt;
        if (weakenT > 0) weakenT -= dt;
    }

    public void update(World w, float dt) {
        updateStatuses(w, dt);
        if (!alive) return;
        if (frozenT > 0 || staggerT > 0 || spawnT > 0) {
            vx *= (1 - 8 * dt);
            vy *= (1 - 8 * dt);
            x += vx * dt;
            y += vy * dt;
            w.clampToArena(this);
            return;
        }

        Player p = w.player;
        float distP = G.dist(x, y, p.x, p.y);
        float tx = p.x, ty = p.y;

        // taunted by decoy?
        if (w.decoyT > 0) {
            tx = w.decoyX;
            ty = w.decoyY;
            distP = G.dist(x, y, tx, ty);
        } else if (disorientT > 0 || (p.stealthT > 0 && type != VOLATILE)) {
            // wander aimlessly
            wanderA += G.rnd(-3f, 3f) * dt;
            tx = x + (float) Math.cos(wanderA) * 200;
            ty = y + (float) Math.sin(wanderA) * 200;
        }

        float mm = moveMul(w) * (elite ? 1.05f : 1f);
        float ang = G.angleTo(x, y, tx, ty);

        switch (type) {
            case SPITTER: {
                // keep distance, volley
                float want = 430;
                float dir = distP > want + 60 ? 1 : (distP < want - 60 ? -1 : 0);
                // strafe
                float strafe = (float) Math.sin(w.time * 0.9f + phaseSeed) * 0.7f;
                vx = ((float) Math.cos(ang) * dir + (float) Math.cos(ang + Math.PI / 2) * strafe) * speed * mm;
                vy = ((float) Math.sin(ang) * dir + (float) Math.sin(ang + Math.PI / 2) * strafe) * speed * mm;
                atkCd -= dt;
                if (atkCd <= 0 && disorientT <= 0 && p.stealthT <= 0 && distP < 760) {
                    windup += dt;
                    vx *= 0.2f;
                    vy *= 0.2f;
                    if (windup > 0.55f) {
                        windup = 0;
                        atkCd = elite ? 1.5f : G.rnd(2.2f, 3.2f);
                        int shots = w.altPattern ? 3 : 1;
                        float dmg = shotDmg * (w.altPattern ? 0.55f : 1f) * w.enemyDmgMul * damageDealtMul();
                        for (int i = 0; i < shots; i++) {
                            float sa = ang + (i - (shots - 1) / 2f) * 0.18f;
                            w.enemyShot(x, y, sa, 520, dmg, tint, false);
                        }
                        w.game.sfx.playVar("shot_blue", 0.25f);
                    }
                } else {
                    windup = 0;
                }
                break;
            }
            case BRUTE: {
                if (chargeT > 0) {
                    chargeT -= dt;
                    vx = chargeDx * 640 * mm;
                    vy = chargeDy * 640 * mm;
                    if (G.rnd() < dt * 20) {
                        w.fx.spawn(x, y, G.rnd(-40, 40), G.rnd(-40, 40), 0.3f, 6, tint, 0);
                    }
                } else if (windup > 0) {
                    windup -= dt;
                    vx *= 0.1f;
                    vy *= 0.1f;
                    if (windup <= 0) {
                        chargeT = 0.55f;
                        chargeDx = (float) Math.cos(ang);
                        chargeDy = (float) Math.sin(ang);
                        w.game.sfx.playVar("dash", 0.6f);
                    }
                } else {
                    vx = (float) Math.cos(ang) * speed * mm;
                    vy = (float) Math.sin(ang) * speed * mm;
                    atkCd -= dt;
                    if (atkCd <= 0 && distP < 420 && distP > 120 && disorientT <= 0 && p.stealthT <= 0) {
                        windup = 0.7f;
                        atkCd = G.rnd(3.5f, 5f);
                    }
                }
                break;
            }
            case VOLATILE: {
                vx = (float) Math.cos(ang) * speed * mm;
                vy = (float) Math.sin(ang) * speed * mm;
                if (distP < r + p.r + 12) {
                    explode(w);
                    return;
                }
                if (G.rnd() < dt * 6) {
                    w.fx.spawn(x, y, 0, 0, 0.3f, 5, tint, 0);
                }
                break;
            }
            case WARDEN: {
                // hang back, pulse shields to allies
                float want = 520;
                float dir = distP > want + 40 ? 1 : (distP < want - 40 ? -0.8f : 0);
                vx = (float) Math.cos(ang) * dir * speed * mm;
                vy = (float) Math.sin(ang) * dir * speed * mm;
                atkCd -= dt;
                if (atkCd <= 0) {
                    atkCd = 5f;
                    w.wardenPulse(this);
                }
                break;
            }
            default: { // HUSK
                float zig = w.altPattern ? (float) Math.sin(w.time * 5f + phaseSeed) * 0.8f : 0f;
                vx = ((float) Math.cos(ang) + (float) Math.cos(ang + Math.PI / 2) * zig) * speed * mm;
                vy = ((float) Math.sin(ang) + (float) Math.sin(ang + Math.PI / 2) * zig) * speed * mm;
                break;
            }
        }

        x += vx * dt;
        y += vy * dt;
        w.clampToArena(this);

        // contact damage
        if (distP < r + p.r && frozenT <= 0) {
            float dd = contactDmg * w.enemyDmgMul * damageDealtMul();
            if (type == BRUTE && chargeT > 0) dd *= 1.6f;
            if (w.hurtPlayer(dd, x, y) && type == BRUTE) {
                chargeT = 0;
                atkCd = Math.max(atkCd, 1.2f);
            }
        }
    }

    public void explode(World w) {
        if (!alive) return;
        alive = false;
        w.onVolatileExploded(this);
    }

    public void draw(Canvas c, World w) {
        float bob = (float) Math.sin(w.time * 3f + phaseSeed) * 3f;
        int alpha = 255;
        if (spawnT > 0) alpha = (int) (255 * (1f - spawnT / 0.5f));
        // Hollow Sign modifier: distant enemies fade
        if (w.opId == 4) {
            float d = G.dist(x, y, w.player.x, w.player.y);
            if (d > 620) alpha = Math.min(alpha, 50);
        }
        // Haunted whispers: cloak when player idle
        if (w.opId == 5 && w.playerIdleT > 1f) alpha = Math.min(alpha, 60);

        int body = Palette.mix(tintDark, tint, hitFlash > 0 ? 1f : 0.25f);
        body = Palette.withAlpha(body, alpha);
        int edge = Palette.withAlpha(tint, alpha);

        if (frozenT > 0) {
            G.glow(c, x, y, r * 2.1f, Palette.withAlpha(Palette.BLUE, 60));
        }
        switch (type) {
            case SPITTER: {
                G.glow(c, x, y, r * 1.8f, Palette.withAlpha(tint, alpha / 5));
                G.circle(c, x, y + bob, r, body);
                G.ring(c, x, y + bob, r, 3f, edge);
                G.circle(c, x + (float) Math.cos(windup * 20) * 4, y + bob, r * 0.4f,
                        Palette.withAlpha(0xFFFFFFFF, alpha / 2));
                if (windup > 0) {
                    G.ring(c, x, y + bob, r + 8 + windup * 20, 2f, Palette.withAlpha(0xFFFFFFFF, 130));
                }
                break;
            }
            case BRUTE: {
                G.glow(c, x, y, r * 1.7f, Palette.withAlpha(tint, alpha / 6));
                G.shard(c, x, y + bob, r * 1.9f, r * 1.9f, 0.78f, body, edge);
                G.shard(c, x, y + bob, r * 1.1f, r * 1.1f, -0.3f, Palette.withAlpha(tintDark, alpha), edge);
                if (windup > 0) {
                    float a2 = G.angleTo(x, y, w.player.x, w.player.y);
                    G.P.setStyle(Paint.Style.STROKE);
                    G.P.setStrokeWidth(3f);
                    G.P.setColor(Palette.withAlpha(Palette.DANGER, 150));
                    c.drawLine(x, y, x + (float) Math.cos(a2) * 500, y + (float) Math.sin(a2) * 500, G.P);
                    G.P.setStyle(Paint.Style.FILL);
                }
                break;
            }
            case VOLATILE: {
                float pulse = 1f + (float) Math.sin(w.time * 10f + phaseSeed) * 0.15f;
                G.glow(c, x, y, r * 2.6f * pulse, Palette.withAlpha(tint, alpha / 3));
                G.circle(c, x, y, r * pulse, body);
                G.ring(c, x, y, r * pulse, 2.5f, edge);
                break;
            }
            case WARDEN: {
                G.glow(c, x, y, r * 2f, Palette.withAlpha(tint, alpha / 4));
                G.shard(c, x, y + bob, r * 1.5f, r * 2.1f, 0f, body, edge);
                if (shieldHp > 0) {
                    G.ring(c, x, y + bob, r + 12, 3f, Palette.withAlpha(0xFFFFFFFF, 70));
                }
                break;
            }
            default: {
                G.shard(c, x, y + bob, r * 1.4f, r * 1.9f,
                        (float) Math.atan2(vy, vx) + 1.57f, body, edge);
                break;
            }
        }
        if (elite) {
            G.ring(c, x, y, r + 7, 2.5f, Palette.withAlpha(Palette.GOLD, (int) (alpha * 0.8f)));
        }

        drawStatusPips(c, alpha);

        // hp bar (small, only when hurt)
        if (hp < maxHp && !isBoss()) {
            float bw = r * 2.2f;
            G.rr(c, x - bw / 2, y - r - 14, x + bw / 2, y - r - 9, 2, Palette.withAlpha(0xFF000000, 120));
            float f = G.clamp(hp / maxHp, 0, 1);
            G.rr(c, x - bw / 2, y - r - 14, x - bw / 2 + bw * f, y - r - 9, 2,
                    Palette.withAlpha(tint, 220));
        }
    }

    protected void drawStatusPips(Canvas c, int alpha) {
        float px = x - r, py = y + r + 8;
        if (burnT > 0) {
            G.circle(c, px, py, 5, Palette.withAlpha(Palette.RED, alpha));
            px += 13;
        }
        if (bleedStacks > 0) {
            for (int i = 0; i < bleedStacks; i++) {
                G.circle(c, px, py, 3.5f, Palette.withAlpha(Palette.GREEN, alpha));
                px += 8;
            }
            px += 5;
        }
        if (chillStacks > 0) {
            for (int i = 0; i < chillStacks; i++) {
                G.circle(c, px, py, 3.5f, Palette.withAlpha(Palette.BLUE, alpha));
                px += 8;
            }
        }
        if (frozenT > 0) {
            G.ring(c, x, y, r + 3, 2.5f, Palette.withAlpha(Palette.BLUE, 200));
        }
        if (hemorrhage) {
            G.ring(c, x, y, r + 5, 2f, Palette.withAlpha(Palette.GREEN, 160));
        }
        if (shieldHp > 0 && type != WARDEN) {
            G.ring(c, x, y, r + 10, 2.5f, Palette.withAlpha(0xFFFFFFFF, 80));
        }
    }
}
