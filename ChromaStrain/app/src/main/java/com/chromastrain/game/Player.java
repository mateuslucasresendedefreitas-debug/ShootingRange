package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/** The Free Runner. One body, three strain kits (faithful to the codex). */
public class Player {

    public final int faction;
    private final World w;

    public float x, y, r = 26;
    public float aim;               // radians
    public boolean firing;
    public float hp, maxHp;
    public float baseSpeed = 340;   // green runs slightly hotter (set in ctor)

    // upgrade-derived
    public float dmgMul = 1f;
    public float speedMul = 1f;
    public float cdMul = 1f;
    public float gadgetMul = 1f;

    // weapon state
    private float shotT;
    private int shotCount;          // Embermaw heat cycle / burst counting
    private int burstLeft;
    private float burstT;
    private int burstNumber;        // Needlewraith neurofracture cycle

    // melee — secondary attack, one distinct mechanic per strain
    public float meleeCd;
    public int comboIdx;             // exposed for HUD combo pips
    public float swingAnim;
    private float swingAngle;
    private boolean meleeBuffered;   // a tap during cooldown queues; fires the instant it clears
    private float comboIdleT;        // resets the green/blue combo chain if you wait too long
    public float meleeHoldT;         // red: how long the attack button has been held
    public boolean meleeCharging;    // red: past the charge threshold, ready to release
    public float chargeCd;           // red: cooldown for the charged smash (separate from meleeCd)

    // usage counters (tutorial step detection; harmless elsewhere)
    public int shotsFired, meleeUses, chargeUses, comboUses, skillUses, gadgetUses, doseUses;

    // skill / gadget
    public float skillCd;
    public float gadgetCd;

    // dose
    public float doseMeter;         // 0..100
    public float doseT;             // active time left
    public float withdrawalT;
    public boolean doseReady() { return doseMeter >= 100 && doseT <= 0 && withdrawalT <= 0; }

    // statuses
    public float stealthT;
    public boolean stealthStrike;   // next attack guaranteed crit
    public float exitSpeedT;
    public float iframes;
    public float painEchoCd;
    public float sinceHurt;         // spinal bloom
    public boolean bloomShield;
    public int dataCharges;
    public float dataT;
    public float hurtFlash;
    public float dashT;

    /** Codex set bonus (Ashblood Forge / Jadestone Warrior / Mind's Anchor),
     *  forged by clearing this faction's HUNT operation. */
    public final boolean setBonus;

    public Player(World w, int faction, Save save) {
        this.w = w;
        this.faction = faction;
        setBonus = save.cleared[faction * 3 + Ops.SLOT_HUNT];
        if (faction == Strain.GREEN) baseSpeed = 360;
        maxHp = 1000 * (1f + 0.12f * save.up[0]);
        hp = maxHp;
        dmgMul = 1f + 0.08f * save.up[1];
        speedMul = 1f + 0.05f * save.up[2];
        cdMul = 1f - 0.06f * save.up[3];
        gadgetMul = 1f + 0.10f * save.up[3];
    }

    public float critChance() {
        float c = faction == Strain.GREEN ? 0.22f : 0.12f;
        return c;
    }

    public float totalDmgMul() {
        float m = dmgMul;
        if (doseT > 0 && faction == Strain.RED) m *= 1.4f;
        m *= 1f + 0.03f * dataCharges;
        return m;
    }

    public float moveSpeed() {
        float s = baseSpeed * speedMul;
        if (doseT > 0 && faction == Strain.GREEN) s *= 1.25f;
        if (exitSpeedT > 0) s *= 1.5f;
        if (bloomShield) s *= 1.12f;
        if (withdrawalT > 0 && faction == Strain.RED) s *= 0.75f;
        if (doseT > 0 && faction == Strain.BLUE) s *= 0.9f; // world slows more
        return s;
    }

    public void update(float dt, float mvx, float mvy, float aimX, float aimY, boolean wantFire) {
        // timers
        if (meleeCd > 0) {
            meleeCd -= dt;
            if (meleeCd <= 0 && meleeBuffered) {
                meleeBuffered = false;
                fireMeleeNow();
            }
        }
        if (chargeCd > 0) chargeCd -= dt;
        if (comboIdx > 0) {
            comboIdleT += dt;
            if (comboIdleT > comboWindow()) comboIdx = 0;
        }
        float cdTick = dt * ((doseT > 0 && faction == Strain.BLUE) ? 2f : 1f);
        if (skillCd > 0) skillCd -= cdTick;
        if (gadgetCd > 0) gadgetCd -= cdTick;
        if (doseT > 0) {
            doseT -= dt;
            if (doseT <= 0) {
                withdrawalT = 4f;
                w.game.sfx.play("hurt", 0.4f, 0.7f);
            }
        }
        if (withdrawalT > 0) withdrawalT -= dt;
        if (stealthT > 0) {
            stealthT -= dt;
            if (stealthT <= 0) exitStealth();
        }
        if (exitSpeedT > 0) exitSpeedT -= dt;
        if (iframes > 0) iframes -= dt;
        if (painEchoCd > 0) painEchoCd -= dt;
        if (hurtFlash > 0) hurtFlash -= dt;
        if (swingAnim > 0) swingAnim -= dt * 3.2f;
        if (dashT > 0) dashT -= dt;
        if (dataT > 0) {
            dataT -= dt;
            if (dataT <= 0) dataCharges = 0;
        }
        sinceHurt += dt;
        if (faction == Strain.GREEN && sinceHurt > 3f) {
            bloomShield = true;
        }

        // movement
        float sp = moveSpeed();
        x += mvx * sp * dt;
        y += mvy * sp * dt;
        w.clampToArenaPlayer(this);
        if ((mvx != 0 || mvy != 0)) {
            w.playerIdleT = 0;
            if (stealthT > 0 && G.rnd() < dt * 14) {
                w.fx.spawn(x, y, 0, 0, 0.35f, r * 0.5f, Palette.withAlpha(Strain.color(faction), 60), 3);
            }
        }

        // aiming
        boolean aiming = G.len(aimX, aimY) > 0.30f;
        if (aiming) {
            aim = (float) Math.atan2(aimY, aimX);
            // gentle magnetism: pull toward a target that is almost on the reticle
            Enemy near = w.nearestEnemyInCone(x, y, aim, 620, 0.16f);
            if (near != null) {
                aim = G.turnToward(aim, G.angleTo(x, y, near.x, near.y), 0.09f);
            }
        } else if (mvx != 0 || mvy != 0) {
            aim = (float) Math.atan2(mvy, mvx);
        }
        firing = wantFire && aiming;

        // firing
        if (shotT > 0) shotT -= dt;
        float rateMul = (withdrawalT > 0 && faction == Strain.GREEN) ? 0.75f : 1f;
        if (burstLeft > 0) {
            burstT -= dt;
            if (burstT <= 0) {
                burstT = 0.065f;
                burstLeft--;
                fireOne(burstNumber % 3 == 0 && burstLeft == 0);
            }
        } else if (firing && shotT <= 0) {
            float[] gun = Strain.GUN[faction];
            shotT = 1f / (gun[2] * rateMul);
            if (faction == Strain.GREEN) {
                burstLeft = 3;
                burstT = 0;
                burstNumber++;
            } else {
                shotCount++;
                fireOne(false);
            }
        }
    }

    private void fireOne(boolean neuroBurst) {
        shotsFired++;
        float[] gun = Strain.GUN[faction];
        float dmg = G.rnd(gun[0], gun[1]) * totalDmgMul();
        boolean crit = G.rnd() < critChance() || (doseT > 0 && faction == Strain.GREEN);
        if (stealthStrike) {
            crit = true;
            stealthStrike = false;
            exitStealth();
        }
        if (crit) dmg *= 1.8f;
        float spread = gun[4];
        float a = aim + G.rnd(-spread, spread);
        Bullet b = w.obtainBullet();
        b.fromPlayer = true;
        b.x = x + (float) Math.cos(aim) * (r + 14);
        b.y = y + (float) Math.sin(aim) * (r + 14);
        b.vx = (float) Math.cos(a) * gun[3];
        b.vy = (float) Math.sin(a) * gun[3];
        b.dmg = dmg;
        b.crit = crit;
        b.color = Strain.color(faction);
        b.r = 9;

        String sndName;
        if (faction == Strain.RED) {
            sndName = "shot_red";
            if (shotCount % 4 == 0) { // Heat Vent Cycle
                b.burn = true;
                b.pierce = 3;
                b.r = 13;
                b.dmg *= 1.15f;
                b.color = 0xFFFFB050;
                w.game.sfx.playVar("burn", 0.5f);
            }
        } else if (faction == Strain.GREEN) {
            sndName = "shot_green";
            if (neuroBurst) {
                b.disrupt = true;
                b.color = 0xFFB8FFC8;
            }
        } else {
            sndName = "shot_blue";
            b.pierce = 2;
            b.chill = 1;
            b.r = 11;
            b.life = 2.2f;
        }
        if (stealthT > 0) exitStealth();
        w.game.sfx.playVar(sndName, faction == Strain.GREEN ? 0.35f : 0.5f);
        w.cam.kick((float) Math.cos(aim), (float) Math.sin(aim), faction == Strain.BLUE ? 6f : 3.4f);
        w.fx.spawn(b.x, b.y, b.vx * 0.12f, b.vy * 0.12f, 0.12f, 10, b.color, 1);
    }

    // ------------------------------------------------------------- actions

    /** Attack button pressed. Always gives immediate feedback: fires now if
     *  ready, or buffers a single queued swing that fires the instant the
     *  cooldown clears — a tap is never silently dropped. */
    public void meleeDown() {
        if (faction == Strain.RED) {
            meleeHoldT = 0.0001f;
            meleeCharging = false;
        }
        if (meleeCd > 0) {
            if (!meleeBuffered) {
                meleeBuffered = true;
                w.game.sfx.play("ui_tap", 0.35f, 1.4f);
            }
            return;
        }
        fireMeleeNow();
    }

    /** Legacy/bot entry point — equivalent to a single tap-and-release. */
    public void tryMelee() {
        meleeDown();
    }

    /** Red only: called every frame the attack button stays held. */
    public void meleeHeld(float dt) {
        if (faction != Strain.RED || meleeHoldT <= 0) return;
        meleeHoldT += dt;
        if (meleeHoldT > 0.40f) meleeCharging = true;
    }

    /** Red only: attack button released — unleashes the charged smash if held long enough. */
    public void meleeRelease() {
        if (faction == Strain.RED && meleeCharging && chargeCd <= 0) {
            fireChargedSmash();
            chargeCd = 3.0f;
        }
        meleeHoldT = 0;
        meleeCharging = false;
    }

    private float comboWindow() {
        if (faction == Strain.GREEN) return 0.65f;
        if (faction == Strain.BLUE) return 0.85f;
        return 0.5f;
    }

    private void fireMeleeNow() {
        float[] m = Strain.MELEE[faction];
        meleeCd = 1f / m[2];
        swingAnim = 1f;
        swingAngle = aim;
        comboIdleT = 0;
        comboIdx++;
        meleeUses++;

        // Green (Ghost Cut) and Blue (Combo Surge) pay off on the 3rd chained
        // hit; Red never chains — its identity is the hold-release charge.
        boolean finisher = faction != Strain.RED && comboIdx % 3 == 0;

        float dmg = G.rnd(m[0], m[1]) * totalDmgMul();
        if (faction == Strain.RED) {
            // Bloodfire Memory: red melee scales with missing HP
            float missing = 1f - hp / maxHp;
            dmg *= 1f + missing * 0.5f;
        }
        if (faction == Strain.BLUE) {
            // System Overclock: +3% per active buff
            int buffs = (doseT > 0 ? 1 : 0) + (bloomShield ? 1 : 0) + dataCharges;
            dmg *= 1f + 0.03f * buffs;
            if (finisher) dmg *= 1.35f; // Combo Surge payoff
        }
        if (faction == Strain.GREEN && finisher) dmg *= 1.4f; // Ghost Cut payoff

        boolean crit = G.rnd() < critChance() + 0.08f || (doseT > 0 && faction == Strain.GREEN);
        if (faction == Strain.GREEN && finisher) crit = true; // Ghost Cut always crits
        if (stealthStrike) {
            crit = true;
            stealthStrike = false;
            exitStealth();
        }
        if (crit) dmg *= 1.8f;

        if (faction == Strain.GREEN) {
            // Whisperfangs: short dash toward aim; Ghost Cut dashes further, behind the target
            float dashDist = finisher ? 150 : 90;
            x += (float) Math.cos(aim) * dashDist;
            y += (float) Math.sin(aim) * dashDist;
            dashT = 0.15f;
            w.clampToArenaPlayer(this);
        }

        float range = m[3] * (finisher ? 1.3f : 1f);
        float arc = m[4] * (finisher ? 1.3f : 1f);
        boolean killedAny = w.meleeSweep(this, aim, range, arc, dmg, crit, finisher);

        if (finisher && faction == Strain.GREEN) {
            comboUses++;
            if (killedAny) stealthT = Math.max(stealthT, 2f); // Ghost Cut: stealth on a kill
            w.game.sfx.play("skill_green", 0.6f, 1.3f);
            w.fx.burst(x, y, 14, 260, 0.4f, 7, Palette.GREEN, 3);
        }
        if (finisher && faction == Strain.BLUE) {
            comboUses++;
            w.comboStunPulse(this, 145, dmg * 0.4f); // Combo Surge: kinetic stun pulse
            w.game.sfx.play("skill_blue", 0.6f, 1.2f);
        }

        w.game.sfx.playVar(faction == Strain.RED ? "melee_red"
                : (faction == Strain.GREEN ? "melee_green" : "melee_blue"), 0.7f);
        w.cam.shake(faction == Strain.RED ? 7f : (finisher ? 9f : 4f));
        w.game.haptic(finisher ? 30 : 16, finisher ? 150 : 90);
        if (stealthT > 0) exitStealth();
    }

    private void fireChargedSmash() {
        float[] m = Strain.MELEE[faction];
        float missing = 1f - hp / maxHp;
        float dmg = G.rnd(m[0], m[1]) * totalDmgMul() * (1f + missing * 0.5f) * 2.0f;
        boolean crit = G.rnd() < critChance() + 0.15f;
        if (crit) dmg *= 1.8f;
        swingAnim = 1f;
        swingAngle = aim;
        w.meleeSweep(this, aim, m[3] * 1.5f, m[4] * 1.35f, dmg, crit, true);
        w.cam.shake(17f);
        w.game.haptic(55, 230);
        w.game.sfx.play("melee_red", 1f, 0.7f);
        w.fx.burst(x, y, 22, 320, 0.6f, 10, Palette.RED, 2);
        chargeUses++;
    }

    public void trySkill() {
        if (skillCd > 0) return;
        if (withdrawalT > 0 && faction == Strain.BLUE) {
            w.game.sfx.play("ui_deny", 0.7f, 1f);
            return;
        }
        skillCd = Strain.SKILL_CD[faction] * cdMul;
        skillUses++;
        if (faction == Strain.RED) {
            // Seismic Fist
            float radius = 260;
            if (w.playerInFireZone()) radius *= 1.25f;
            w.seismicFist(this, radius, 420 * totalDmgMul());
            w.game.sfx.play("skill_red", 1f, 1f);
        } else if (faction == Strain.GREEN) {
            // Phantom Vein
            stealthT = 4f;
            stealthStrike = true;
            w.game.sfx.play("skill_green", 0.9f, 1f);
            w.fx.burst(x, y, 14, 160, 0.5f, 8, Palette.GREEN, 3);
        } else {
            // Overcharge Wave
            w.overchargeWave(this, 300, 260 * totalDmgMul());
            gadgetCd -= Strain.GADGET_CD[Strain.BLUE] * 0.35f;
            w.game.sfx.play("skill_blue", 1f, 1f);
        }
        w.game.haptic(30, 120);
    }

    public void tryGadget() {
        if (gadgetCd > 0) return;
        gadgetCd = Strain.GADGET_CD[faction] * cdMul;
        gadgetUses++;
        float tx = x + (float) Math.cos(aim) * 300;
        float ty = y + (float) Math.sin(aim) * 300;
        if (faction == Strain.RED) {
            w.addFireZone(tx, ty, 140 * gadgetMul, 6f, 95 * totalDmgMul());
        } else if (faction == Strain.GREEN) {
            w.deployDecoy(x + (float) Math.cos(aim) * 120, y + (float) Math.sin(aim) * 120, 4f);
        } else {
            w.addTrapZone(tx, ty, 150 * gadgetMul, 6f, 130 * totalDmgMul());
        }
        w.game.sfx.play("gadget", 0.9f, 1f);
    }

    public void tryDose() {
        if (!doseReady()) {
            w.game.sfx.play("ui_deny", 0.6f, 1f);
            return;
        }
        doseMeter = 0;
        doseT = faction == Strain.BLUE ? 6f : 8f;
        doseUses++;
        w.game.sfx.play("dose", 1f, 1f);
        w.game.haptic(60, 200);
        w.fx.burst(x, y, 26, 320, 0.8f, 9, Palette.DOSE, 2);
        w.cam.shake(10f);
        w.doseFlash = 1f;
    }

    private void exitStealth() {
        if (stealthT > 0 || exitSpeedT <= 0) {
            exitSpeedT = 3f;
        }
        stealthT = 0;
    }

    public void onKill() {
        doseMeter = Math.min(100, doseMeter + 12);
        if (faction == Strain.BLUE) {
            dataCharges = Math.min(5, dataCharges + 1);
            dataT = 6f;
        }
    }

    /** @return true if damage actually landed. */
    public boolean hurt(float amount, float sx, float sy) {
        if (iframes > 0 || dashT > 0) return false;
        if (stealthT > 0) return false;
        float a = amount;
        if (doseT > 0 && faction == Strain.RED) a *= 0.8f;
        if (bloomShield) {
            a *= 0.6f;
            bloomShield = false;
        }
        sinceHurt = 0;
        hp -= a;
        iframes = 0.55f;
        hurtFlash = 0.3f;
        w.game.sfx.playVar("hurt", 0.9f);
        w.game.haptic(40, 180);
        w.cam.shake(9f);

        // Pain Echo
        if (faction == Strain.RED && hp > 0 && hp / maxHp < 0.30f && painEchoCd <= 0) {
            painEchoCd = 5f;
            w.painEcho(this, 240, 300 * totalDmgMul());
        }
        return true;
    }

    public void meleeLifesteal(float dealt) {
        if (doseT > 0 && faction == Strain.RED) {
            hp = Math.min(maxHp, hp + dealt * 0.2f);
        }
    }

    public void draw(Canvas c) {
        int col = Strain.color(faction);
        int alpha = stealthT > 0 ? 80 : 255;

        // dose aura
        if (doseT > 0) {
            G.glow(c, x, y, r * 3f, Palette.withAlpha(Palette.DOSE, 70));
        }
        if (bloomShield && faction == Strain.GREEN) {
            G.ring(c, x, y, r + 9, 2f, Palette.withAlpha(Palette.GREEN, 90));
        }

        G.glow(c, x, y, r * 2.2f, Palette.withAlpha(col, alpha / 6));

        // body: rotated shard toward aim
        G.shard(c, x, y, r * 1.5f, r * 2.0f, aim + (float) Math.PI / 2,
                Palette.withAlpha(Palette.mix(Strain.colorDark(faction), col, 0.45f), alpha),
                Palette.withAlpha(col, alpha));
        // core
        G.circle(c, x, y, r * 0.42f, Palette.withAlpha(0xFFFFFFFF, (int) (alpha * 0.85f)));

        // weapon direction tick
        float gx = x + (float) Math.cos(aim) * (r + 10);
        float gy = y + (float) Math.sin(aim) * (r + 10);
        G.P.setStyle(Paint.Style.STROKE);
        G.P.setStrokeWidth(6f);
        G.P.setColor(Palette.withAlpha(col, alpha));
        c.drawLine(x + (float) Math.cos(aim) * r * 0.5f, y + (float) Math.sin(aim) * r * 0.5f, gx, gy, G.P);
        G.P.setStyle(Paint.Style.FILL);

        // melee swing arc
        if (swingAnim > 0) {
            float[] m = Strain.MELEE[faction];
            float prog = 1f - swingAnim;
            float a0 = swingAngle - m[4] / 2 + m[4] * prog;
            G.P.setStyle(Paint.Style.STROKE);
            G.P.setStrokeWidth(10f * swingAnim + 2);
            G.P.setColor(Palette.withAlpha(0xFFFFFFFF, (int) (170 * swingAnim)));
            c.drawArc(x - m[3], y - m[3], x + m[3], y + m[3],
                    (float) Math.toDegrees(swingAngle - m[4] / 2),
                    (float) Math.toDegrees(m[4] * prog), false, G.P);
            G.P.setStyle(Paint.Style.FILL);
        }

        if (hurtFlash > 0) {
            G.ring(c, x, y, r + 4, 3f, Palette.withAlpha(Palette.DANGER, (int) (255 * hurtFlash / 0.3f)));
        }

        if (meleeCharging) {
            float cg = G.clamp((meleeHoldT - 0.40f) / 0.6f, 0, 1);
            G.glow(c, x, y, r * (2.0f + cg * 1.8f), Palette.withAlpha(Palette.GOLD, (int) (90 + 130 * cg)));
        }
    }
}
