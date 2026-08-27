import android.content.Context;

import com.chromastrain.game.Boss;
import com.chromastrain.game.Enemy;
import com.chromastrain.game.G;
import com.chromastrain.game.Game;
import com.chromastrain.game.Ops;
import com.chromastrain.game.Player;
import com.chromastrain.game.World;

/**
 * Headless integration test: a bot plays every operation with every faction.
 * Exercises the full simulation layer (waves, statuses, zones, rings, all nine
 * boss scripts, dose cycles) at 60 Hz to catch runtime crashes and stalls.
 */
public class HeadlessSim {

    static final float DT = 1f / 60f;

    static final int GOD = 0;      // must reach VICTORY (validates every boss script)
    static final int MORTAL = 1;   // informational difficulty telemetry
    static final int PASSIVE = 2;  // must reach DEFEAT

    public static void main(String[] args) {
        int failures = 0;
        // god sweep: every faction beats every operation -> all boss scripts run to death
        for (int faction = 0; faction < 3; faction++) {
            for (int op = 0; op < 9; op++) {
                String tag = "god  f" + faction + "/op" + op;
                try {
                    String r = playRun(faction, op, GOD, 60 * 300);
                    System.out.println(tag + " -> " + r);
                    if (!r.startsWith("VICTORY")) failures++;
                } catch (Throwable t) {
                    failures++;
                    System.out.println(tag + " -> CRASH: " + t);
                    t.printStackTrace(System.out);
                }
            }
        }
        // mortal telemetry (dumb bot; humans play far better) — no assertions
        for (int op = 0; op < 9; op += 4) {
            for (int faction = 0; faction < 3; faction++) {
                try {
                    System.out.println("mort f" + faction + "/op" + op + " -> "
                            + playRun(faction, op, MORTAL, 60 * 240));
                } catch (Throwable t) {
                    failures++;
                    System.out.println("mort CRASH: " + t);
                    t.printStackTrace(System.out);
                }
            }
        }
        // defeat path sanity
        try {
            String r = playRun(0, 1, PASSIVE, 60 * 240);
            System.out.println("passive f0/op1 -> " + r);
            if (!r.startsWith("DEFEAT")) failures++;
        } catch (Throwable t) {
            failures++;
            System.out.println("passive -> CRASH: " + t);
            t.printStackTrace(System.out);
        }
        System.out.println(failures == 0 ? "SIM: ALL RUNS OK" : "SIM FAILURES: " + failures);
        System.exit(failures == 0 ? 0 : 1);
    }

    static String playRun(int faction, int op, int mode, int maxSteps) {
        boolean fight = mode != PASSIVE;
        Game game = new Game(new Context());
        game.sfx.sfxOn = false;
        game.sfx.musicOn = false;
        game.save.faction = faction;
        game.save.doses[faction] = 1; // exercise the stabilized-dose path
        game.save.cleared[faction * 3 + 2] = mode == GOD; // exercise set bonuses
        game.resize(1600, 720);
        World w = new World(game, op);
        Player p = w.player;

        int step = 0;
        float angle = 0;
        while (step < maxSteps) {
            step++;
            float mvx = 0, mvy = 0, aimx = 0, aimy = 0;
            boolean fire = false;
            if (fight) {
                // orbit the arena center, kite, aim at closest threat
                angle += DT * 0.9f;
                float tx = w.arenaW / 2 + (float) Math.cos(angle) * 380;
                float ty = w.arenaH / 2 + (float) Math.sin(angle) * 240;
                float dx = tx - p.x, dy = ty - p.y;
                float l = G.len(dx, dy);
                if (l > 20) {
                    mvx = dx / l;
                    mvy = dy / l;
                }
                Enemy target = w.boss != null && w.boss.alive ? w.boss
                        : (w.enemies.isEmpty() ? null : w.enemies.get(0));
                if (target != null) {
                    float ax = target.x - p.x, ay = target.y - p.y;
                    float al = Math.max(1, G.len(ax, ay));
                    aimx = ax / al;
                    aimy = ay / al;
                    fire = true;
                }
                // mash abilities to exercise every code path
                if (step % 47 == 0) p.tryMelee();
                if (step % 133 == 0) p.trySkill();
                if (step % 171 == 0) p.tryGadget();
                if (step % 209 == 0) p.tryDose();
            }
            p.update(DT, mvx, mvy, aimx, aimy, fire);
            w.update(DT);

            if (mode == GOD && p.hp < p.maxHp) {
                p.hp = p.maxHp; // godmode: survive to exercise full boss scripts
            }
            // bot "dps assist" so long fights converge: chunk the boss like
            // sustained fire would, through the real damage path
            if (fight && w.boss != null && w.boss.alive && step % 6 == 0) {
                w.lastHitWasMelee = step % 12 == 0;
                w.boss.damage(w, mode == GOD ? 300 : 220, false);
            }
            // god bot also clears stalled waves the way a player repositioning would
            if (mode == GOD && w.state == World.STATE_WAVE && w.waveT > 90
                    && !w.enemies.isEmpty() && step % 8 == 0) {
                w.enemies.get(0).damage(w, 400, false);
            }

            // invariants
            if (Float.isNaN(p.x) || Float.isNaN(p.y) || Float.isNaN(p.hp)) {
                return "NAN at step " + step;
            }
            if (w.enemies.size() > 400) {
                return "ENEMY FLOOD " + w.enemies.size();
            }
            if (w.state == World.STATE_VICTORY) {
                return "VICTORY step=" + step + " kills=" + w.kills + " score=" + w.score;
            }
            if (w.state == World.STATE_DEFEAT) {
                return "DEFEAT step=" + step + " wave=" + w.wave;
            }
        }
        return "TIMEOUT state=" + w.state + " wave=" + w.wave
                + (w.boss != null ? " bossHp=" + (int) w.boss.hp : "");
    }
}
