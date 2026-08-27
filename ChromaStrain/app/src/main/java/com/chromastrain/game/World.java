package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/** One operation run: arena, waves, boss, projectiles, zones, camera, score. */
public class World {

    public static final int STATE_INTRO = 0;
    public static final int STATE_WAVE = 1;
    public static final int STATE_INTERLUDE = 2;
    public static final int STATE_BOSS_INTRO = 3;
    public static final int STATE_BOSS = 4;
    public static final int STATE_VICTORY = 5;
    public static final int STATE_DEFEAT = 6;
    public static final int STATE_SANDBOX = 7;   // training room: no waves, no boss, no defeat

    public final Game game;
    public final boolean tutorial;
    public final int opId;
    public final int factionOp;
    public final int slot;
    public final Player player;
    public final Particles fx = new Particles();
    public final Cam cam = new Cam();

    public float arenaW = 2000, arenaH = 1300;
    public float time;
    public int state = STATE_INTRO;
    public float stateT;
    public int wave;             // 1-based current wave
    public final int totalWaves;
    public Boss boss;

    public final ArrayList<Enemy> enemies = new ArrayList<Enemy>();
    private final ArrayList<Bullet> bullets = new ArrayList<Bullet>();
    private final ArrayList<Bullet> bulletPool = new ArrayList<Bullet>();
    public final ArrayList<Zone> zones = new ArrayList<Zone>();
    public final ArrayList<Ring> rings = new ArrayList<Ring>();
    private final ArrayList<float[]> spawnQueue = new ArrayList<float[]>(); // t,x,y,type,elite
    private final ArrayList<Pickup> pickups = new ArrayList<Pickup>();
    private final ArrayList<Dmg> dmgTexts = new ArrayList<Dmg>();
    private final ArrayList<float[]> pillars = new ArrayList<float[]>();   // x,y,r

    // modifiers / global muls
    public float enemySpeedMul = 1f;
    public float enemyDmgMul = 1f;
    public boolean altPattern;
    public float waveElapsed;
    public float waveT;           // time in current wave (anti-stall)
    public float overclockT;      // op6 ramp
    public float surgeT;          // op1 pressure rings
    public float rampCycleT;      // op2 fury/fatigue
    public boolean rampFury;
    public float virusSpawnT;     // op7

    public float playerIdleT;
    public boolean lastHitWasMelee;
    public float hitstop;
    public float doseFlash;
    public float slowmo = 1f;

    // decoy
    public float decoyT, decoyX, decoyY;

    // banner
    public String bannerText = "";
    public int bannerColor;
    public float bannerT;

    // score & loot
    public int score;
    public int scoreMul100 = 100;   // percent
    public int shardsFound;
    public int nodesFound;
    public int kills;
    public float elapsed;
    public boolean tookDamage;

    public World(Game game, int opId) {
        this.game = game;
        this.tutorial = false;
        this.opId = opId;
        factionOp = Ops.faction(opId);
        slot = Ops.slot(opId);
        totalWaves = Ops.WAVES[opId];
        player = new Player(this, game.save.faction, game.save);
        player.x = arenaW / 2;
        player.y = arenaH / 2;
        // stabilized dose: start charged
        if (game.save.doses[game.save.faction] > 0) {
            game.save.doses[game.save.faction]--;
            player.doseMeter = 100;
            game.save.flush();
        }
        cam.x = player.x;
        cam.y = player.y;
        stateT = 2.4f;
        // pillars: symmetric crystal cover
        long seed = opId * 7919L + 13;
        java.util.Random rr = new java.util.Random(seed);
        for (int i = 0; i < 3; i++) {
            float px = 330 + rr.nextFloat() * (arenaW / 2 - 500);
            float py = 260 + rr.nextFloat() * (arenaH - 520);
            float pr = 46 + rr.nextFloat() * 26;
            pillars.add(new float[]{px, py, pr});
            pillars.add(new float[]{arenaW - px, arenaH - py, pr});
        }
        showBanner(Ops.NAME[opId], Strain.color(factionOp));
    }

    /** Training room: a pressure-free arena with stationary, harmless dummies. */
    public World(Game game, boolean tutorialMode) {
        this.game = game;
        this.tutorial = true;
        this.opId = -1;
        factionOp = game.save.faction;
        slot = 0;
        totalWaves = 0;
        player = new Player(this, game.save.faction, game.save);
        arenaW = 1400;
        arenaH = 900;
        player.x = arenaW / 2;
        player.y = arenaH * 0.62f;
        player.doseMeter = 100;
        cam.x = player.x;
        cam.y = player.y;
        state = STATE_SANDBOX;
        spawnDummies();
    }

    private void spawnDummies() {
        addDummy(arenaW * 0.5f, arenaH * 0.28f);
        addDummy(arenaW * 0.74f, arenaH * 0.42f);
        addDummy(arenaW * 0.26f, arenaH * 0.42f);
    }

    private void addDummy(float x, float y) {
        Enemy e = new Enemy();
        e.init(Enemy.HUSK, x, y, 1f, 1f, factionOp, false);
        e.speed = 0;
        e.contactDmg = 0;
        e.hp = e.maxHp = 5000;
        enemies.add(e);
    }

    private float dummyRespawnT = 1.2f;

    private void updateTutorial(float dt) {
        // fast, unconditional dose refill so every ability can be tested freely
        player.doseMeter = Math.min(100, player.doseMeter + dt * 22f);
        if (enemies.isEmpty()) {
            dummyRespawnT -= dt;
            if (dummyRespawnT <= 0) {
                spawnDummies();
                dummyRespawnT = 1.0f;
            }
        }
    }

    // ================================================================ update

    public void update(float dt) {
        if (hitstop > 0) {
            hitstop -= dt;
            cam.update(dt, player);
            return;
        }
        time += dt;
        if (state != STATE_VICTORY && state != STATE_DEFEAT) elapsed += dt;
        if (bannerT > 0) bannerT -= dt;
        if (doseFlash > 0) doseFlash -= dt * 2f;
        playerIdleT += dt;

        float wdt = dt; // enemy-side dt (blue dose dilation)
        if (player.doseT > 0 && player.faction == Strain.BLUE) {
            wdt = dt * 0.45f;
        }
        if (slowmo < 1f) {
            wdt *= slowmo;
            dt *= G.lerp(slowmo, 1f, 0.5f);
        }

        if (state == STATE_WAVE) waveT += wdt;
        if (beamHurtCd > 0) beamHurtCd -= wdt;
        if (tutorial) updateTutorial(wdt);
        updateModifiers(wdt);
        updateStates(dt);

        // spawn queue
        for (int i = spawnQueue.size() - 1; i >= 0; i--) {
            float[] s = spawnQueue.get(i);
            s[0] -= wdt;
            if (s[0] <= 0) {
                Enemy e = new Enemy();
                float hpMul = Ops.TIER_HP[slot] * (1f + 0.12f * (wave - 1));
                float dmMul = Ops.TIER_DMG[slot] * (1f + 0.05f * (wave - 1));
                e.init((int) s[3], s[1], s[2], hpMul, dmMul, factionOp, s[4] > 0);
                enemies.add(e);
                spawnQueue.remove(i);
            }
        }

        // entities
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(this, wdt);
            if (!e.alive) enemies.remove(i);
        }
        if (boss != null && boss.alive) {
            boss.update(this, wdt);
        }
        separateEnemies();

        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            float bdt = b.fromPlayer ? dt : wdt;
            b.x += b.vx * bdt;
            b.y += b.vy * bdt;
            b.life -= bdt;
            boolean dead = b.life <= 0 || hitPillar(b.x, b.y, b.r);
            if (!dead) dead = collideBullet(b);
            if (b.x < 20 || b.x > arenaW - 20 || b.y < 20 || b.y > arenaH - 20) dead = true;
            if (dead) {
                b.alive = false;
                bullets.remove(i);
                bulletPool.add(b);
            }
        }

        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone z = zones.get(i);
            if (!z.update(this, wdt)) zones.remove(i);
        }
        for (int i = rings.size() - 1; i >= 0; i--) {
            Ring rg = rings.get(i);
            if (!rg.update(this, wdt)) rings.remove(i);
        }
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup pk = pickups.get(i);
            if (!pk.update(this, dt)) pickups.remove(i);
        }
        for (int i = dmgTexts.size() - 1; i >= 0; i--) {
            Dmg d = dmgTexts.get(i);
            d.life -= dt;
            d.y -= 46 * dt;
            if (d.life <= 0) dmgTexts.remove(i);
        }
        if (decoyT > 0) {
            decoyT -= wdt;
            if (decoyT <= 0) {
                // flash detonation
                fx.burst(decoyX, decoyY, 20, 300, 0.5f, 8, Palette.GREEN, 0);
                game.sfx.play("explode", 0.7f, 1.4f);
                for (int i = 0; i < enemies.size(); i++) {
                    Enemy e = enemies.get(i);
                    if (G.dist(e.x, e.y, decoyX, decoyY) < 230) {
                        e.disorientT = 2f;
                        e.chillT = 3f;
                        e.chillStacks = Math.min(2, e.chillStacks + 1);
                    }
                }
            }
        }

        fx.update(dt);
        cam.shakeEnabled = game.save.shake;
        cam.update(dt, player);
    }

    private void updateModifiers(float wdt) {
        enemySpeedMul = 1f;
        enemyDmgMul = 1f;
        switch (opId) {
            case 0: { // Berserker Mode
                if (state == STATE_WAVE) waveElapsed += wdt;
                float ramp = Math.min(0.6f, waveElapsed * 0.035f);
                enemySpeedMul += ramp * 0.5f;
                enemyDmgMul += ramp;
                break;
            }
            case 1: { // Pressure Surge
                if (state == STATE_WAVE || state == STATE_BOSS) {
                    surgeT += wdt;
                    if (surgeT > 14f) {
                        surgeT = 0;
                        echoRing(arenaW / 2, arenaH / 2, G.rnd(0, 6.28f), 1f, 90 * Ops.TIER_DMG[slot]);
                        showBanner("PRESSURE SURGE", Palette.RED);
                    }
                }
                break;
            }
            case 2: { // Rampage cycles (waves only)
                if (state == STATE_WAVE) {
                    rampCycleT -= wdt;
                    if (rampCycleT <= 0) {
                        rampFury = !rampFury;
                        rampCycleT = rampFury ? 7f : 5f;
                    }
                    enemySpeedMul *= rampFury ? 1.35f : 0.75f;
                }
                break;
            }
            case 6: { // Overclock surge
                if (state == STATE_WAVE || state == STATE_BOSS) {
                    overclockT += wdt;
                    float ramp = Math.min(0.5f, overclockT * 0.01f);
                    enemySpeedMul += ramp;
                    scoreMul100 = 100 + (int) (overclockT * 1.2f);
                }
                break;
            }
            case 7: { // Hive logic: ambient virus zones
                if (state == STATE_WAVE) {
                    virusSpawnT += wdt;
                    if (virusSpawnT > 12f) {
                        virusSpawnT = 0;
                        addVirusZone();
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void updateStates(float dt) {
        switch (state) {
            case STATE_INTRO: {
                stateT -= dt;
                if (stateT <= 0) {
                    startWave(1);
                }
                break;
            }
            case STATE_WAVE: {
                if (enemies.isEmpty() && spawnQueue.isEmpty()) {
                    score += (int) (250 * scoreMul100 / 100f);
                    if (!tookDamage) score += 300;
                    tookDamage = false;
                    if (wave >= totalWaves) {
                        state = STATE_BOSS_INTRO;
                        stateT = 2.2f;
                        showBanner(Ops.BOSS_NAME[opId], Palette.DANGER);
                        game.sfx.play("boss_roar", 1f, 1f);
                    } else {
                        state = STATE_INTERLUDE;
                        stateT = 2.4f;
                        // interlude heal orb
                        spawnPickup(player.x + G.rnd(-200, 200), player.y + G.rnd(-160, 160), 2, 0);
                        showBanner("WAVE " + wave + " CLEARED", Palette.INK);
                        game.sfx.play("wave", 0.8f, 1.2f);
                    }
                }
                break;
            }
            case STATE_INTERLUDE: {
                stateT -= dt;
                if (stateT <= 0) startWave(wave + 1);
                break;
            }
            case STATE_BOSS_INTRO: {
                stateT -= dt;
                if (stateT <= 0) {
                    state = STATE_BOSS;
                    boss = new Boss(opId, arenaW / 2, 240);
                    game.sfx.music(musicFor(true));
                }
                break;
            }
            case STATE_BOSS: {
                if (boss != null && !boss.alive) {
                    state = STATE_VICTORY;
                    stateT = 2.6f;
                    slowmo = 0.25f;
                    game.sfx.play("victory", 1f, 1f);
                    game.haptic(80, 220);
                }
                break;
            }
            case STATE_VICTORY:
            case STATE_DEFEAT: {
                stateT -= dt;
                slowmo = G.lerp(slowmo, 1f, dt * 2f);
                break;
            }
            default:
                break;
        }
    }

    public String musicFor(boolean bossPhase) {
        return bossPhase ? "boss" : "op" + factionOp;
    }

    private void startWave(int n) {
        wave = n;
        state = STATE_WAVE;
        waveElapsed = 0;
        waveT = 0;
        tookDamage = false;
        altPattern = (opId == 3) && (n % 2 == 0); // Flickering Reality
        showBanner("WAVE " + n + " / " + totalWaves, Strain.color(factionOp));
        game.sfx.play("wave", 0.9f, 1f);

        int idx = n - 1;
        float countMul = slot == Ops.SLOT_RAID ? 1.25f : (slot == Ops.SLOT_HUNT ? 1.5f : 1f);
        int husks = (int) ((4 + idx * 2) * countMul);
        int spitters = (int) ((1 + idx) * countMul);
        int brutes = idx >= 1 ? 1 + (idx - 1) / 2 : 0;
        int vols = idx >= 1 ? 2 : 0;
        int wardens = idx >= 2 && slot != Ops.SLOT_TRIALS ? 1 : (idx >= 3 ? 1 : 0);
        queueSpawns(Enemy.HUSK, husks, false);
        queueSpawns(Enemy.SPITTER, spitters, false);
        queueSpawns(Enemy.BRUTE, brutes, false);
        queueSpawns(Enemy.VOLATILE, vols, false);
        queueSpawns(Enemy.WARDEN, wardens, false);
        boolean elite = (slot == Ops.SLOT_TRIALS && n == totalWaves)
                || (slot != Ops.SLOT_TRIALS && n >= totalWaves - 1);
        if (elite) queueSpawns(Enemy.HUSK, 1, true);
    }

    private void queueSpawns(int type, int count, boolean elite) {
        for (int i = 0; i < count; i++) {
            float[] s = new float[5];
            s[0] = 0.8f + i * 0.35f + G.rnd(0, 0.5f);
            // spawn at edges, away from player
            int side = G.rndi(0, 3);
            float sx = side == 0 ? 90 : (side == 1 ? arenaW - 90 : G.rnd(120, arenaW - 120));
            float sy = side <= 1 ? G.rnd(120, arenaH - 120) : (side == 2 ? 90 : arenaH - 90);
            if (G.dist(sx, sy, player.x, player.y) < 380) {
                sx = arenaW - sx;
                sy = arenaH - sy;
            }
            s[1] = sx;
            s[2] = sy;
            s[3] = type;
            s[4] = elite ? 1 : 0;
            spawnQueue.add(s);
        }
    }

    public void summonAdds(int n) {
        queueSpawns(Enemy.HUSK, n - 1, false);
        queueSpawns(Enemy.SPITTER, 1, false);
        showBanner("REINFORCEMENTS", Palette.INK_DIM);
    }

    // =============================================================== combat

    private int bulletSerial;

    public Bullet obtainBullet() {
        Bullet b = bulletPool.isEmpty() ? new Bullet() : bulletPool.remove(bulletPool.size() - 1);
        b.reset();
        b.id = ++bulletSerial;
        bullets.add(b);
        return b;
    }

    public void enemyShot(float x, float y, float ang, float speed, float dmg, int color, boolean heavy) {
        Bullet b = obtainBullet();
        b.fromPlayer = false;
        b.x = x;
        b.y = y;
        b.vx = (float) Math.cos(ang) * speed;
        b.vy = (float) Math.sin(ang) * speed;
        b.dmg = dmg;
        b.color = color;
        b.heavy = heavy;
        b.r = heavy ? 13 : 10;
        b.life = 3.2f;
    }

    private boolean collideBullet(Bullet b) {
        if (b.fromPlayer) {
            Enemy hit = null;
            if (boss != null && boss.alive && boss.lastBulletId != b.id
                    && G.dist(b.x, b.y, boss.x, boss.y) < b.r + boss.r) {
                hit = boss;
            }
            if (hit == null) {
                for (int i = 0; i < enemies.size(); i++) {
                    Enemy e = enemies.get(i);
                    if (e.lastBulletId == b.id || e.spawnT > 0) continue;
                    if (G.dist(b.x, b.y, e.x, e.y) < b.r + e.r) {
                        hit = e;
                        break;
                    }
                }
            }
            if (hit == null) return false;

            if (hit instanceof Minion && ((Minion) hit).reflect && !((Minion) hit).marked) {
                fx.burst(b.x, b.y, 5, 160, 0.3f, 5, 0xFFFFFFFF, 1);
                addDmgText(hit.x, hit.y - hit.r - 20, "IMMUNE", Palette.INK_DIM, false);
                return true;
            }

            lastHitWasMelee = false;
            float dealt = hit.damage(this, b.dmg, b.crit);
            addDmgText(hit.x + G.rnd(-14, 14), hit.y - hit.r - 8,
                    String.valueOf((int) dealt), b.crit ? Palette.GOLD : 0xFFFFFFFF, b.crit);
            fx.burst(b.x, b.y, b.crit ? 8 : 4, 200, 0.3f, 5, b.color, 1);
            game.sfx.playVar(b.crit ? "crit" : "hit", b.crit ? 0.8f : 0.45f);
            if (b.burn) hit.applyBurn(this, b.dmg * 0.30f);
            if (b.chill > 0) hit.applyChill(this, b.chill);
            if (b.disrupt) hit.disruptT = 5f;
            if (b.crit && player.faction == Strain.GREEN) hit.applyBleed(this);
            if (b.crit) hitstop = Math.max(hitstop, 0.025f);

            hit.lastBulletId = b.id;
            if (b.pierce > 0) {
                b.pierce--;
                return false;
            }
            return true;
        } else {
            Player p = player;
            if (G.dist(b.x, b.y, p.x, p.y) < b.r + p.r) {
                hurtPlayer(b.dmg, b.x, b.y);
                return true;
            }
            // player bullets can be blocked by decoy? no. done.
            return false;
        }
    }

    /** @return true if damage landed on the player. */
    public boolean hurtPlayer(float dmg, float sx, float sy) {
        boolean landed = player.hurt(dmg, sx, sy);
        if (landed) {
            tookDamage = true;
            addDmgText(player.x, player.y - player.r - 16, String.valueOf((int) dmg), Palette.DANGER, false);
            if (player.hp <= 0 && state != STATE_DEFEAT && state != STATE_VICTORY) {
                state = STATE_DEFEAT;
                stateT = 2.6f;
                slowmo = 0.3f;
                game.sfx.play("defeat", 1f, 1f);
                fx.burst(player.x, player.y, 30, 400, 1f, 9, Strain.color(player.faction), 2);
            }
        }
        return landed;
    }

    /** Green's short-range strike: a dash-through hit along the segment the
     *  player just moved, not a cone at the destination — you cut through
     *  whoever is BETWEEN start and end, matching a real gap-closing stab.
     *  @return true if this hit killed at least one enemy. */
    public boolean meleeLineSweep(Player p, float x0, float y0, float x1, float y1,
                                  float thickness, float dmg, boolean crit, boolean finisher) {
        int hits = 0;
        boolean killedAny = false;
        for (int i = -1; i < enemies.size(); i++) {
            Enemy e = i < 0 ? boss : enemies.get(i);
            if (e == null || !e.alive || e.spawnT > 0) continue;
            if (G.pointSegDist(e.x, e.y, x0, y0, x1, y1) > thickness + e.r) continue;

            lastHitWasMelee = true;
            float dealt = e.damage(this, dmg, crit);
            hits++;
            if (!e.alive) killedAny = true;
            p.meleeLifesteal(dealt);
            addDmgText(e.x + G.rnd(-12, 12), e.y - e.r - 8, String.valueOf((int) dealt),
                    crit ? Palette.GOLD : 0xFFFFFFFF, crit);
            if (e.alive) {
                if (crit) {
                    e.applyBleed(this);
                    if (p.setBonus) { // Jadestone Warrior: bleed spreads
                        for (int k = 0; k < enemies.size(); k++) {
                            Enemy o = enemies.get(k);
                            if (o != e && o.alive && o.spawnT <= 0
                                    && G.dist(e.x, e.y, o.x, o.y) < 150) {
                                o.applyBleed(this);
                            }
                        }
                    }
                }
                if (finisher) e.staggerT = Math.max(e.staggerT, 0.9f);
                float a = G.angleTo(x0, y0, e.x, e.y);
                e.x += (float) Math.cos(a) * 16;
                e.y += (float) Math.sin(a) * 16;
                if (e instanceof Minion) ((Minion) e).marked = true;
            }
            fx.burst(e.x, e.y, 6, 240, 0.3f, 6, Palette.GREEN, 1);
        }
        if (hits > 0) {
            hitstop = Math.max(hitstop, 0.03f);
            game.haptic(20, 120);
        }
        return killedAny;
    }

    /** @return true if this sweep killed at least one enemy. */
    public boolean meleeSweep(Player p, float ang, float range, float arc, float dmg,
                              boolean crit, boolean finisher) {
        int hits = 0;
        boolean killedAny = false;
        for (int i = -1; i < enemies.size(); i++) {
            Enemy e = i < 0 ? boss : enemies.get(i);
            if (e == null || !e.alive || e.spawnT > 0) continue;
            float d = G.dist(p.x, p.y, e.x, e.y);
            if (d > range + e.r) continue;
            float a = G.angleTo(p.x, p.y, e.x, e.y);
            float diff = a - ang;
            while (diff > Math.PI) diff -= (float) (Math.PI * 2);
            while (diff < -Math.PI) diff += (float) (Math.PI * 2);
            if (Math.abs(diff) > arc / 2 && d > e.r + 40) continue;

            lastHitWasMelee = true;
            float dealt = e.damage(this, dmg, crit);
            hits++;
            if (!e.alive) killedAny = true;
            p.meleeLifesteal(dealt);
            addDmgText(e.x + G.rnd(-12, 12), e.y - e.r - 8, String.valueOf((int) dealt),
                    crit ? Palette.GOLD : 0xFFFFFFFF, crit);
            if (e.alive) {
                if (p.faction == Strain.RED) e.staggerT = Math.max(e.staggerT, 0.5f);
                if (finisher) e.staggerT = Math.max(e.staggerT, 0.9f);
                if (crit && p.faction == Strain.GREEN) {
                    e.applyBleed(this);
                    if (p.setBonus) { // Jadestone Warrior: bleed spreads
                        for (int k = 0; k < enemies.size(); k++) {
                            Enemy o = enemies.get(k);
                            if (o != e && o.alive && o.spawnT <= 0
                                    && G.dist(e.x, e.y, o.x, o.y) < 150) {
                                o.applyBleed(this);
                            }
                        }
                    }
                }
                if (p.faction == Strain.RED && p.hp / p.maxHp < 0.3f && G.rnd() < 0.35f) {
                    e.applyBurn(this, dmg * 0.2f); // Bloodfire ignite
                }
                // knockback
                e.x += (float) Math.cos(a) * 26;
                e.y += (float) Math.sin(a) * 26;
                if (e instanceof Minion) ((Minion) e).marked = true;
            }
            fx.burst(e.x, e.y, 6, 240, 0.3f, 6, Strain.color(p.faction), 1);
        }
        if (hits > 0) {
            hitstop = Math.max(hitstop, p.faction == Strain.RED ? 0.05f : 0.03f);
            game.haptic(20, 120);
        }
        return killedAny;
    }

    public void bossMeleeSweep(Boss b, float ang, float range, float arc, float dmg) {
        float d = G.dist(b.x, b.y, player.x, player.y);
        if (d > range + player.r) return;
        float a = G.angleTo(b.x, b.y, player.x, player.y);
        float diff = a - ang;
        while (diff > Math.PI) diff -= (float) (Math.PI * 2);
        while (diff < -Math.PI) diff += (float) (Math.PI * 2);
        if (Math.abs(diff) > arc / 2) return;
        hurtPlayer(dmg, b.x, b.y);
    }

    public void seismicFist(Player p, float radius, float dmg) {
        cam.shake(13f);
        hitstop = 0.05f;
        fx.burst(p.x, p.y, 26, 420, 0.6f, 8, Palette.RED, 2);
        rings.add(Ring.visual(p.x, p.y, radius));
        for (int i = -1; i < enemies.size(); i++) {
            Enemy e = i < 0 ? boss : enemies.get(i);
            if (e == null || !e.alive || e.spawnT > 0) continue;
            float d = G.dist(p.x, p.y, e.x, e.y);
            if (d > radius + e.r) continue;
            float mult = e.burnT > 0 ? 1.5f : 1f; // ignite synergy
            lastHitWasMelee = true;
            float dealt = e.damage(this, dmg * mult, false);
            addDmgText(e.x, e.y - e.r - 8, String.valueOf((int) dealt), Palette.RED, true);
            if (e.alive) {
                e.staggerT = Math.max(e.staggerT, 1.1f);
                float a = G.angleTo(p.x, p.y, e.x, e.y);
                e.x += (float) Math.cos(a) * 60;
                e.y += (float) Math.sin(a) * 60;
            }
        }
    }

    public void overchargeWave(Player p, float radius, float dmg) {
        cam.shake(10f);
        fx.burst(p.x, p.y, 22, 380, 0.5f, 7, Palette.BLUE, 1);
        rings.add(Ring.visual(p.x, p.y, radius));
        for (int i = -1; i < enemies.size(); i++) {
            Enemy e = i < 0 ? boss : enemies.get(i);
            if (e == null || !e.alive || e.spawnT > 0) continue;
            if (G.dist(p.x, p.y, e.x, e.y) > radius + e.r) continue;
            lastHitWasMelee = false;
            float dealt = e.damage(this, dmg, false);
            addDmgText(e.x, e.y - e.r - 8, String.valueOf((int) dealt), Palette.BLUE, false);
            if (e.alive) {
                e.windup = 0;
                e.chargeT = 0;
                e.disorientT = Math.max(e.disorientT, 2.5f);
                e.weakenT = 5f;
                e.staggerT = Math.max(e.staggerT, 0.4f);
            }
        }
    }

    public void painEcho(Player p, float radius, float dmg) {
        game.sfx.play("skill_red", 0.8f, 0.7f);
        fx.burst(p.x, p.y, 18, 360, 0.5f, 7, Palette.DANGER, 2);
        rings.add(Ring.visual(p.x, p.y, radius));
        for (int i = -1; i < enemies.size(); i++) {
            Enemy e = i < 0 ? boss : enemies.get(i);
            if (e == null || !e.alive) continue;
            if (G.dist(p.x, p.y, e.x, e.y) > radius + e.r) continue;
            lastHitWasMelee = true;
            e.damage(this, dmg, false);
            if (e.alive) {
                float a = G.angleTo(p.x, p.y, e.x, e.y);
                e.x += (float) Math.cos(a) * 90;
                e.y += (float) Math.sin(a) * 90;
                e.staggerT = Math.max(e.staggerT, 0.6f);
            }
        }
    }

    public void wardenPulse(Enemy warden) {
        fx.burst(warden.x, warden.y, 10, 200, 0.5f, 6, 0xFFFFFFFF, 3);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e == warden || !e.alive) continue;
            if (G.dist(warden.x, warden.y, e.x, e.y) < 260) {
                e.shieldHp = Math.min(300 * Ops.TIER_HP[slot], e.shieldHp + 150 * Ops.TIER_HP[slot]);
            }
        }
    }

    public void onEnemyKilled(Enemy e) {
        kills++;
        player.onKill();
        // Ashblood Forge: kills on burning enemies vent the skill
        if (player.setBonus && player.faction == Strain.RED && e.burnT > 0) {
            player.skillCd = Math.max(0, player.skillCd - 1f);
        }
        game.sfx.playVar("kill", 0.7f);
        fx.burst(e.x, e.y, e.isBoss() ? 40 : 12, e.isBoss() ? 460 : 260, 0.6f,
                e.isBoss() ? 11 : 7, e.tint, 2);
        cam.shake(e.isBoss() ? 16f : (e.elite ? 8f : 3f));

        if (e instanceof Minion) {
            Minion m = (Minion) e;
            if (m.kind == Minion.CLONE && m.owner != null && m.owner.alive) {
                m.owner.enrage += 0.08f;
                m.owner.hp = Math.min(m.owner.maxHp, m.owner.hp + m.owner.maxHp * 0.02f);
                addDmgText(m.owner.x, m.owner.y - m.owner.r - 24, "EMPOWERED", Palette.DANGER, true);
            }
            return; // minions drop nothing
        }

        int base = e.isBoss() ? 2500 * (slot + 1) : (e.elite ? 350 : 100 + slot * 40);
        score += (int) (base * scoreMul100 / 100f);

        if (e.isBoss()) {
            for (int i = 0; i < 3 + slot; i++) {
                spawnPickup(e.x + G.rnd(-60, 60), e.y + G.rnd(-60, 60), 1, 0);
            }
            for (int i = 0; i < 4; i++) {
                spawnPickup(e.x + G.rnd(-80, 80), e.y + G.rnd(-80, 80), 0, G.rndi(10, 22));
            }
        } else {
            float roll = G.rnd();
            if (roll < 0.10f) spawnPickup(e.x, e.y, 1, 0);
            else if (roll < 0.32f) spawnPickup(e.x, e.y, 0, G.rndi(4, 11));
            else if (roll < 0.40f) spawnPickup(e.x, e.y, 2, 0);
        }
    }

    public void onVolatileExploded(Enemy e) {
        game.sfx.playVar("explode", 0.9f);
        cam.shake(8f);
        fx.burst(e.x, e.y, 20, 340, 0.5f, 8, e.tint, 0);
        rings.add(Ring.visual(e.x, e.y, 150));
        if (G.dist(e.x, e.y, player.x, player.y) < 150 + player.r) {
            hurtPlayer(e.contactDmg * enemyDmgMul, e.x, e.y);
        }
        kills++;
        player.onKill();
        score += (int) (80 * scoreMul100 / 100f);
    }

    // ============================================================ zones etc.

    public void addFireZone(float x, float y, float r, float dur, float dps) {
        Zone z = new Zone();
        z.kind = Zone.FIRE;
        z.x = x; z.y = y; z.r = r; z.dur = dur; z.power = dps;
        zones.add(z);
    }

    public void addTrapZone(float x, float y, float r, float dur, float dmg) {
        Zone z = new Zone();
        z.kind = Zone.TRAP;
        z.x = x; z.y = y; z.r = r; z.dur = dur; z.power = dmg;
        zones.add(z);
    }

    public void dataTrail(float x, float y, float dps) {
        if (G.rnd() < 0.25f) {
            Zone z = new Zone();
            z.kind = Zone.TRAIL;
            z.x = x; z.y = y; z.r = 42; z.dur = 2.5f; z.power = dps * 4;
            zones.add(z);
        }
    }

    private boolean virusTaught;

    public void addVirusZone() {
        Zone z = new Zone();
        z.kind = Zone.VIRUS;
        z.x = G.rnd(260, arenaW - 260);
        z.y = G.rnd(220, arenaH - 220);
        z.r = 110;
        z.dur = 999f;
        zones.add(z);
        if (!virusTaught) {
            virusTaught = true;
            showBanner("VIRUS ZONE — STAND IN IT TO PURGE", Palette.BLUE);
        }
    }

    public int matureVirusCount() {
        int n = 0;
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            if (z.kind == Zone.VIRUS && z.mature) n++;
        }
        return n;
    }

    public boolean playerInFireZone() {
        for (int i = 0; i < zones.size(); i++) {
            Zone z = zones.get(i);
            if (z.kind == Zone.FIRE && G.dist(player.x, player.y, z.x, z.y) < z.r) return true;
        }
        return false;
    }

    public void deployDecoy(float x, float y, float dur) {
        decoyX = x;
        decoyY = y;
        decoyT = dur;
    }

    public void shockRing(float x, float y, float r0, float maxR, float dmg) {
        Ring rg = new Ring();
        rg.cx = x; rg.cy = y; rg.radius = r0; rg.maxR = maxR;
        rg.speed = 700; rg.width = 46; rg.dmg = dmg; rg.gap = -1;
        rings.add(rg);
        game.sfx.playVar("explode", 0.8f);
    }

    public void echoRing(float x, float y, float gapA, float widthMul, float dmg) {
        Ring rg = new Ring();
        rg.cx = x; rg.cy = y; rg.radius = 40; rg.maxR = 1500;
        rg.speed = 360; rg.width = 52 * widthMul; rg.dmg = dmg; rg.gap = gapA;
        rings.add(rg);
        game.sfx.play("boss_roar", 0.5f, 1.6f);
    }

    public float beamHurtCd;

    /** Continuous beam: ticks damage on its own cadence, independent of iframes.
     *  The cooldown decays in updateModifiers on world (dilated) time so the
     *  blue dose does not multiply beam ticks. */
    public void beamDamage(Boss b, float ang, float len, float dmgPerTick) {
        float px = player.x - b.x, py = player.y - b.y;
        float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
        float t = G.clamp(px * dx + py * dy, 0, len);
        float cx = b.x + dx * t, cy = b.y + dy * t;
        if (G.dist(player.x, player.y, cx, cy) < player.r + 14 && beamHurtCd <= 0) {
            beamHurtCd = 0.35f;
            float save = player.iframes;
            player.iframes = 0;
            hurtPlayer(dmgPerTick, cx, cy);
            player.iframes = Math.max(player.iframes, save);
        }
    }

    public void spawnPriestessClones(Boss owner, int n) {
        for (int i = 0; i < n; i++) {
            Minion m = new Minion();
            m.initMinion(Minion.CLONE, owner,
                    G.rnd(260, arenaW - 260), G.rnd(220, arenaH - 220), slot);
            enemies.add(m);
        }
    }

    public void despawnClones(Boss owner) {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e instanceof Minion && ((Minion) e).kind == Minion.CLONE) {
                enemies.remove(i);
            }
        }
    }

    public void clonesVolley(Boss owner) {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e instanceof Minion && ((Minion) e).kind == Minion.CLONE) {
                float a = G.angleTo(e.x, e.y, player.x, player.y);
                for (int k = 0; k < 3; k++) {
                    enemyShot(e.x, e.y, a + (k - 1) * 0.2f, 430, boss.shotDmg * 0.5f * enemyDmgMul,
                            e.tint, false);
                }
            }
        }
    }

    public void spawnEchoTurret(float x, float y) {
        int turrets = 0;
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e instanceof Minion && ((Minion) e).kind == Minion.TURRET) turrets++;
        }
        if (turrets >= 2) return;
        Minion m = new Minion();
        m.initMinion(Minion.TURRET, boss, x, y, slot);
        enemies.add(m);
    }

    public void spawnMirrorThorns(Boss owner, int n) {
        for (int i = 0; i < n; i++) {
            Minion m = new Minion();
            m.initMinion(Minion.THORN, owner,
                    owner.x + G.rnd(-200, 200), owner.y + G.rnd(-150, 150), slot);
            enemies.add(m);
        }
    }

    /** Closest living enemy (boss included) within an angular cone of the aim. */
    public Enemy nearestEnemyInCone(float x, float y, float aimAng, float maxDist, float halfCone) {
        Enemy best = null;
        float bestD = maxDist;
        for (int i = -1; i < enemies.size(); i++) {
            Enemy e = i < 0 ? boss : enemies.get(i);
            if (e == null || !e.alive || e.spawnT > 0) continue;
            float d = G.dist(x, y, e.x, e.y);
            if (d > bestD) continue;
            float diff = G.angleTo(x, y, e.x, e.y) - aimAng;
            while (diff > Math.PI) diff -= (float) (Math.PI * 2);
            while (diff < -Math.PI) diff += (float) (Math.PI * 2);
            if (Math.abs(diff) > halfCone) continue;
            best = e;
            bestD = d;
        }
        return best;
    }

    private void spawnPickup(float x, float y, int kind, int amount) {
        Pickup p = new Pickup();
        p.x = G.clamp(x, 60, arenaW - 60);
        p.y = G.clamp(y, 60, arenaH - 60);
        p.kind = kind;
        p.amount = amount;
        pickups.add(p);
    }

    public void addDmgText(float x, float y, String s, int color, boolean big) {
        if (dmgTexts.size() > 40) dmgTexts.remove(0);
        Dmg d = new Dmg();
        d.x = x; d.y = y; d.text = s; d.color = color; d.big = big; d.life = 0.8f;
        dmgTexts.add(d);
    }

    public void showBanner(String text, int color) {
        bannerText = text;
        bannerColor = color;
        bannerT = 2.2f;
    }

    // ========================================================== geometry

    public void clampToArena(Enemy e) {
        e.x = G.clamp(e.x, e.r + 30, arenaW - e.r - 30);
        e.y = G.clamp(e.y, e.r + 30, arenaH - e.r - 30);
        pushOutOfPillars(e);
    }

    public void clampToArenaPlayer(Player p) {
        p.x = G.clamp(p.x, p.r + 30, arenaW - p.r - 30);
        p.y = G.clamp(p.y, p.r + 30, arenaH - p.r - 30);
        for (int i = 0; i < pillars.size(); i++) {
            float[] pl = pillars.get(i);
            float d = G.dist(p.x, p.y, pl[0], pl[1]);
            float min = pl[2] + p.r;
            if (d < min && d > 0.001f) {
                p.x = pl[0] + (p.x - pl[0]) / d * min;
                p.y = pl[1] + (p.y - pl[1]) / d * min;
            }
        }
    }

    private void pushOutOfPillars(Enemy e) {
        for (int i = 0; i < pillars.size(); i++) {
            float[] pl = pillars.get(i);
            float d = G.dist(e.x, e.y, pl[0], pl[1]);
            float min = pl[2] + e.r;
            if (d < min && d > 0.001f) {
                e.x = pl[0] + (e.x - pl[0]) / d * min;
                e.y = pl[1] + (e.y - pl[1]) / d * min;
            }
        }
    }

    private boolean hitPillar(float x, float y, float r) {
        for (int i = 0; i < pillars.size(); i++) {
            float[] pl = pillars.get(i);
            if (G.dist(x, y, pl[0], pl[1]) < pl[2] + r * 0.4f) return true;
        }
        return false;
    }

    private void separateEnemies() {
        int n = enemies.size();
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                float d = G.dist(a.x, a.y, b.x, b.y);
                float min = (a.r + b.r) * 0.85f;
                if (d < min && d > 0.001f) {
                    float push = (min - d) * 0.5f;
                    float nx = (b.x - a.x) / d, ny = (b.y - a.y) / d;
                    a.x -= nx * push;
                    a.y -= ny * push;
                    b.x += nx * push;
                    b.y += ny * push;
                }
            }
        }
    }

    // ============================================================== drawing

    public void draw(Canvas c, float viewW, float viewH) {
        c.save();
        c.translate(viewW / 2 - cam.x + cam.shakeX, viewH / 2 - cam.y + cam.shakeY);

        drawArena(c);
        for (int i = 0; i < zones.size(); i++) zones.get(i).draw(c, this);
        // pickups below entities
        for (int i = 0; i < pickups.size(); i++) pickups.get(i).draw(c, this);
        if (decoyT > 0) drawDecoy(c);
        for (int i = 0; i < enemies.size(); i++) enemies.get(i).draw(c, this);
        if (boss != null && boss.alive) boss.draw(c, this);

        // spawn telegraphs
        for (int i = 0; i < spawnQueue.size(); i++) {
            float[] s = spawnQueue.get(i);
            float t = 1f - G.clamp(s[0] / 1.2f, 0, 1);
            G.ring(c, s[1], s[2], 34 * (1f - t * 0.4f), 3f,
                    Palette.withAlpha(Strain.color(factionOp), (int) (150 * t)));
        }

        player.draw(c);

        // bullets
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            G.glow(c, b.x, b.y, b.r * 2.6f, Palette.withAlpha(b.color, 60));
            G.circle(c, b.x, b.y, b.r * 0.55f, b.fromPlayer ? 0xFFFFFFFF : b.color);
        }

        for (int i = 0; i < rings.size(); i++) rings.get(i).draw(c);
        fx.draw(c);

        // damage numbers
        for (int i = 0; i < dmgTexts.size(); i++) {
            Dmg d = dmgTexts.get(i);
            float a = G.clamp(d.life / 0.8f, 0, 1);
            G.textCB(c, d.text, d.x, d.y, d.big ? 30 : 22,
                    Palette.withAlpha(d.color, (int) (255 * a)));
        }

        c.restore();
    }

    private void drawArena(Canvas c) {
        // floor grid
        Paint p = G.P;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        p.setColor(Palette.GRID);
        for (float gx = 0; gx <= arenaW; gx += 100) {
            c.drawLine(gx, 0, gx, arenaH, p);
        }
        for (float gy = 0; gy <= arenaH; gy += 100) {
            c.drawLine(0, gy, arenaW, gy, p);
        }
        // border
        p.setStrokeWidth(6f);
        p.setColor(Palette.withAlpha(Strain.color(factionOp), 140));
        c.drawRect(20, 20, arenaW - 20, arenaH - 20, p);
        p.setStyle(Paint.Style.FILL);

        // pillars
        for (int i = 0; i < pillars.size(); i++) {
            float[] pl = pillars.get(i);
            G.glow(c, pl[0], pl[1], pl[2] * 2f, Palette.withAlpha(Strain.color(factionOp), 26));
            G.shard(c, pl[0], pl[1], pl[2] * 1.6f, pl[2] * 2.1f, (i * 1.1f) % 3f,
                    Palette.mix(Palette.VOID_TOP, Strain.color(factionOp), 0.22f),
                    Palette.withAlpha(Strain.color(factionOp), 120));
        }
    }

    private void drawDecoy(Canvas c) {
        float blink = (float) Math.sin(time * 12f) * 0.5f + 0.5f;
        int col = Palette.withAlpha(Palette.GREEN, (int) (120 + 80 * blink));
        G.glow(c, decoyX, decoyY, 70, Palette.withAlpha(Palette.GREEN, 50));
        G.shard(c, decoyX, decoyY, 38, 52, 0, Palette.withAlpha(Palette.GREEN_DARK, 160), col);
        G.circle(c, decoyX, decoyY, 10, col);
    }

    // ============================================================== inner types

    public static class Cam {
        public float x, y;
        public float shakeX, shakeY;
        public boolean shakeEnabled = true;
        private float trauma;
        private float kickX, kickY;

        public void shake(float amount) {
            trauma = Math.min(22f, trauma + amount);
        }

        public void kick(float dx, float dy, float amount) {
            kickX -= dx * amount;
            kickY -= dy * amount;
        }

        public void update(float dt, Player p) {
            float lookX = (float) Math.cos(p.aim) * 70;
            float lookY = (float) Math.sin(p.aim) * 70;
            x = G.lerp(x, p.x + lookX + kickX, dt * 6f);
            y = G.lerp(y, p.y + lookY + kickY, dt * 6f);
            kickX *= (1 - 8 * dt);
            kickY *= (1 - 8 * dt);
            trauma = Math.max(0, trauma - dt * 30f);
            float t = shakeEnabled ? trauma * trauma / 484f * 22f : 0f;
            shakeX = G.rnd(-t, t);
            shakeY = G.rnd(-t, t);
        }
    }

    public static class Zone {
        public static final int FIRE = 0;
        public static final int TRAP = 1;
        public static final int TRAIL = 2;
        public static final int VIRUS = 3;

        public int kind;
        public float x, y, r, dur, age, power;
        public float tick;
        public boolean mature;
        public float purge;

        public boolean update(World w, float dt) {
            age += dt;
            dur -= dt;
            if (kind == FIRE) {
                tick -= dt;
                if (tick <= 0) {
                    tick = 0.5f;
                    for (int i = -1; i < w.enemies.size(); i++) {
                        Enemy e = i < 0 ? w.boss : w.enemies.get(i);
                        if (e == null || !e.alive) continue;
                        if (G.dist(x, y, e.x, e.y) < r + e.r) {
                            w.lastHitWasMelee = false;
                            e.damage(w, power * 0.5f, false);
                            e.applyBurn(w, power * 0.35f);
                        }
                    }
                }
                if (G.rnd() < dt * 12) {
                    w.fx.spawn(x + G.rnd(-r, r) * 0.8f, y + G.rnd(-r, r) * 0.8f,
                            G.rnd(-20, 20), -G.rnd(40, 110), 0.5f, 7, Palette.RED, 0);
                }
            } else if (kind == TRAP) {
                tick -= dt;
                if (tick <= 0) {
                    tick = 1.0f;
                    boolean zapped = false;
                    for (int i = -1; i < w.enemies.size(); i++) {
                        Enemy e = i < 0 ? w.boss : w.enemies.get(i);
                        if (e == null || !e.alive) continue;
                        if (G.dist(x, y, e.x, e.y) < r + e.r) {
                            w.lastHitWasMelee = false;
                            e.damage(w, power, false);
                            e.staggerT = Math.max(e.staggerT, 0.35f);
                            w.fx.spawn(e.x, e.y, 0, -60, 0.3f, 6, Palette.BLUE, 1);
                            zapped = true;
                        }
                    }
                    if (zapped) w.game.sfx.playVar("hit", 0.5f);
                }
            } else if (kind == TRAIL) {
                if (G.dist(x, y, w.player.x, w.player.y) < r + w.player.r) {
                    w.hurtPlayer(power * 0.4f, x, y);
                }
            } else if (kind == VIRUS) {
                if (!mature) {
                    if (age > 6f) {
                        mature = true;
                        r = 190;
                    } else {
                        r = 110 + age / 6f * 80f;
                    }
                }
                if (G.dist(x, y, w.player.x, w.player.y) < r) {
                    purge += dt;
                    if (purge > 1.2f) {
                        w.score += (int) (150 * w.scoreMul100 / 100f);
                        w.game.sfx.play("heal", 0.9f, 1.3f);
                        w.fx.burst(x, y, 16, 260, 0.5f, 7, Palette.BLUE, 3);
                        w.addDmgText(x, y - 30, "PURGED +150", Palette.SHARD, true);
                        return false;
                    }
                } else {
                    purge = Math.max(0, purge - dt * 2);
                }
            }
            return dur > 0;
        }

        public void draw(Canvas c, World w) {
            switch (kind) {
                case FIRE: {
                    G.glow(c, x, y, r * 1.25f, Palette.withAlpha(Palette.RED, 70));
                    G.ring(c, x, y, r, 3f, Palette.withAlpha(Palette.RED, 160));
                    break;
                }
                case TRAP: {
                    float pulse = (float) Math.sin(w.time * 6f) * 0.5f + 0.5f;
                    G.ring(c, x, y, r, 2.5f, Palette.withAlpha(Palette.BLUE, 120));
                    G.ring(c, x, y, r * (0.4f + 0.6f * pulse), 1.5f, Palette.withAlpha(Palette.BLUE, 70));
                    break;
                }
                case TRAIL: {
                    G.glow(c, x, y, r * 1.4f, Palette.withAlpha(Palette.BLUE, 60));
                    break;
                }
                case VIRUS: {
                    int col = mature ? Palette.DANGER : Palette.BLUE;
                    G.glow(c, x, y, r, Palette.withAlpha(col, 40));
                    G.ring(c, x, y, r, 3f, Palette.withAlpha(col, 150));
                    if (purge > 0) {
                        G.cooldownSweep(c, x, y, 40, purge / 1.2f, Palette.withAlpha(Palette.SHARD, 180));
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    public static class Ring {
        public float cx, cy, radius, maxR, speed, width, dmg;
        public float gap = -1;      // gap center angle; -1 = full circle
        public boolean hitDone;
        public boolean visualOnly;
        public float life = 99f;

        public static Ring visual(float x, float y, float maxR) {
            Ring r = new Ring();
            r.cx = x; r.cy = y; r.radius = 10; r.maxR = maxR;
            r.speed = maxR * 3.2f; r.visualOnly = true; r.width = 8;
            return r;
        }

        public boolean update(World w, float dt) {
            radius += speed * dt;
            if (!visualOnly && !hitDone) {
                Player p = w.player;
                float d = G.dist(cx, cy, p.x, p.y);
                if (Math.abs(d - radius) < width / 2 + p.r * 0.6f) {
                    boolean inGap = false;
                    if (gap >= 0) {
                        float a = G.angleTo(cx, cy, p.x, p.y);
                        float diff = a - gap;
                        while (diff > Math.PI) diff -= (float) (Math.PI * 2);
                        while (diff < -Math.PI) diff += (float) (Math.PI * 2);
                        inGap = Math.abs(diff) < 0.55f;
                    }
                    if (!inGap) {
                        if (w.hurtPlayer(dmg, cx, cy)) hitDone = true;
                    }
                }
            }
            return radius < maxR;
        }

        public void draw(Canvas c) {
            float t = 1f - radius / maxR;
            int alpha = (int) ((visualOnly ? 160 : 200) * G.clamp(t + 0.3f, 0, 1));
            G.P.setStyle(Paint.Style.STROKE);
            G.P.setStrokeWidth(visualOnly ? 4f : width * 0.5f);
            G.P.setColor(Palette.withAlpha(visualOnly ? 0xFFFFFFFF : Palette.DANGER, alpha));
            if (gap < 0) {
                c.drawCircle(cx, cy, radius, G.P);
            } else {
                float gapDeg = (float) Math.toDegrees(0.55f * 2);
                float startDeg = (float) Math.toDegrees(gap) + gapDeg / 2;
                c.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                        startDeg, 360 - gapDeg, false, G.P);
            }
            G.P.setStyle(Paint.Style.FILL);
        }
    }

    public static class Pickup {
        public int kind;   // 0 shards, 1 node, 2 heal
        public int amount;
        public float x, y, age;

        public boolean update(World w, float dt) {
            age += dt;
            if (age > 20f) return false;
            Player p = w.player;
            float d = G.dist(x, y, p.x, p.y);
            if (d < 170) {
                x += (p.x - x) / d * 320 * dt;
                y += (p.y - y) / d * 320 * dt;
            }
            if (d < p.r + 26) {
                if (kind == 0) {
                    w.shardsFound += amount;
                    w.game.sfx.playVar("pickup", 0.6f);
                } else if (kind == 1) {
                    w.nodesFound++;
                    w.game.sfx.play("pickup", 0.9f, 0.7f);
                    w.addDmgText(x, y - 20, "+1 NODE", Palette.DOSE, true);
                } else {
                    p.hp = Math.min(p.maxHp, p.hp + p.maxHp * 0.15f);
                    w.game.sfx.play("heal", 0.9f, 1f);
                    w.addDmgText(x, y - 20, "+HP", Palette.GREEN, false);
                }
                return false;
            }
            return true;
        }

        public void draw(Canvas c, World w) {
            float bob = (float) Math.sin(w.time * 4f + x * 0.01f) * 4f;
            if (kind == 0) {
                G.glow(c, x, y + bob, 26, Palette.withAlpha(Palette.SHARD, 90));
                G.shard(c, x, y + bob, 14, 20, 0.6f, Palette.withAlpha(Palette.SHARD, 230), 0xFFFFFFFF);
            } else if (kind == 1) {
                G.glow(c, x, y + bob, 34, Palette.withAlpha(Palette.DOSE, 110));
                G.shard(c, x, y + bob, 20, 28, -0.4f, Palette.withAlpha(Palette.DOSE, 240), 0xFFFFFFFF);
            } else {
                G.glow(c, x, y + bob, 26, Palette.withAlpha(Palette.GREEN, 90));
                G.circle(c, x, y + bob, 11, Palette.withAlpha(Palette.GREEN, 230));
                G.textCB(c, "+", x, y + bob + 6, 18, 0xFF083018);
            }
        }
    }

    public static class Dmg {
        public float x, y, life;
        public String text;
        public int color;
        public boolean big;
    }

    /** Boss-owned special minions: priestess clones, mirror thorns, echo turrets. */
    public static class Minion extends Enemy {
        public static final int CLONE = 0;
        public static final int THORN = 1;
        public static final int TURRET = 2;

        public int kind;
        public Boss owner;
        public boolean reflect;
        public boolean marked;
        private float fireT;

        public void initMinion(int kind, Boss owner, float x, float y, int slot) {
            init(HUSK, x, y, 1f, 1f, owner != null ? Ops.faction(owner.opId) : 0, false);
            this.kind = kind;
            this.owner = owner;
            if (kind == CLONE) {
                r = owner.r * 0.9f;
                hp = maxHp = 1;
                speed = 90;
                contactDmg = 40;
            } else if (kind == THORN) {
                r = 26;
                hp = maxHp = 700 * Ops.TIER_HP[slot];
                reflect = true;
                speed = 100;
                contactDmg = 60;
            } else {
                r = 30;
                hp = maxHp = 650 * Ops.TIER_HP[slot];
                speed = 0;
                contactDmg = 0;
                spawnT = 0.7f;
            }
        }

        @Override
        public void update(World w, float dt) {
            updateStatuses(w, dt);
            if (!alive || spawnT > 0 || frozenT > 0 || staggerT > 0) return;
            Player p = w.player;
            float ang = G.angleTo(x, y, p.x, p.y);
            if (kind == CLONE) {
                // drift slowly like the boss
                x += Math.cos(wanderA) * speed * dt;
                y += Math.sin(wanderA) * speed * dt;
                if (G.rnd() < dt) wanderA += G.rnd(-1.5f, 1.5f);
                w.clampToArena(this);
                if (G.dist(x, y, p.x, p.y) < r + p.r) {
                    w.hurtPlayer(contactDmg * w.enemyDmgMul, x, y);
                }
            } else if (kind == THORN) {
                x += Math.cos(ang) * speed * dt;
                y += Math.sin(ang) * speed * dt;
                w.clampToArena(this);
                if (G.dist(x, y, p.x, p.y) < r + p.r) {
                    w.hurtPlayer(contactDmg * w.enemyDmgMul, x, y);
                }
            } else { // TURRET: fire the player's own gun back
                fireT -= dt;
                if (fireT <= 0) {
                    int f = p.faction;
                    if (f == Strain.GREEN) {
                        fireT = 0.8f;
                        for (int i = 0; i < 3; i++) {
                            w.enemyShot(x, y, ang + G.rnd(-0.06f, 0.06f), 900,
                                    45 * w.enemyDmgMul, Strain.color(f), false);
                        }
                    } else if (f == Strain.BLUE) {
                        fireT = 1.1f;
                        w.enemyShot(x, y, ang, 1100, 110 * w.enemyDmgMul, Strain.color(f), true);
                    } else {
                        fireT = 0.55f;
                        w.enemyShot(x, y, ang + G.rnd(-0.04f, 0.04f), 850,
                                75 * w.enemyDmgMul, Strain.color(f), false);
                    }
                    w.game.sfx.playVar("shot_" + (f == 0 ? "red" : (f == 1 ? "green" : "blue")), 0.3f);
                }
            }
        }

        @Override
        public void draw(Canvas c, World w) {
            if (kind == CLONE && owner != null) {
                // looks like the boss, but with a dim core (the "tell")
                float pulse = 1f + (float) Math.sin(w.time * 2.4f + phaseSeed) * 0.05f;
                int body = Palette.mix(tintDark, tint, 0.3f);
                G.shard(c, x - r * 0.5f, y + r * 0.15f, r * 1.1f, r * 1.7f, -0.5f, body, tint);
                G.shard(c, x + r * 0.5f, y + r * 0.2f, r * 1.05f, r * 1.6f, 0.5f, body, tint);
                G.shard(c, x, y - r * 0.1f, r * 1.3f, r * 2.2f * pulse, 0f, body, tint);
                G.circle(c, x, y - r * 0.15f, r * 0.3f, Palette.withAlpha(0xFFFFFFFF, 70));
            } else if (kind == THORN) {
                int col = marked ? tint : 0xFFC8D8E8;
                G.glow(c, x, y, r * 1.8f, Palette.withAlpha(col, 50));
                G.shard(c, x, y, r * 1.5f, r * 1.9f, w.time * 0.8f, Palette.withAlpha(col, 90), col);
                if (!marked) {
                    G.textCB(c, "MARK WITH MELEE", x, y - r - 16, 15, Palette.withAlpha(Palette.INK, 160));
                }
                if (hp < maxHp) {
                    float bw = r * 2.2f;
                    float f = G.clamp(hp / maxHp, 0, 1);
                    G.rr(c, x - bw / 2, y + r + 8, x - bw / 2 + bw * f, y + r + 13, 2,
                            Palette.withAlpha(col, 200));
                }
            } else {
                int col = Strain.color(w.player.faction);
                G.glow(c, x, y, r * 2f, Palette.withAlpha(col, 60));
                G.ring(c, x, y, r, 3f, col);
                G.shard(c, x, y, r * 1.2f, r * 1.5f, w.time * 1.2f, Palette.withAlpha(col, 120), col);
                G.textCB(c, "ECHO", x, y - r - 14, 15, Palette.withAlpha(col, 200));
                if (hp < maxHp) {
                    float bw = r * 2.2f;
                    float f = G.clamp(hp / maxHp, 0, 1);
                    G.rr(c, x - bw / 2, y + r + 8, x - bw / 2 + bw * f, y + r + 13, 2,
                            Palette.withAlpha(col, 200));
                }
            }
        }
    }
}
