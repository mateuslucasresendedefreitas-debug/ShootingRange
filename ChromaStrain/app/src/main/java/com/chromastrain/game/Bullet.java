package com.chromastrain.game;

/** Projectile (player or enemy). Pooled by World. */
public class Bullet {

    public boolean alive;
    public boolean fromPlayer;
    public float x, y, vx, vy;
    public float r;
    public float dmg;
    public boolean crit;
    public int pierce;          // remaining pierces
    public float life;
    public int color;
    public boolean burn;        // Embermaw incendiary payload
    public int chill;           // Glacivore chill stacks to apply
    public boolean disrupt;     // Needlewraith neurofracture payload
    public boolean heavy;       // enemy heavy shot (bigger telegraph)
    public float slowFrac;      // enemy chilling shots etc.
    public Enemy lastHit;       // avoids double-hitting the same target on pierce

    public void reset() {
        lastHit = null;
        alive = true;
        fromPlayer = false;
        crit = false;
        burn = false;
        disrupt = false;
        heavy = false;
        chill = 0;
        pierce = 0;
        slowFrac = 0;
        life = 1.6f;
        r = 9;
    }
}
