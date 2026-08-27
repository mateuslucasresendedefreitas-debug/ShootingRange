package com.chromastrain.game;

import android.graphics.Canvas;

/** A full-screen state (title, hub, run, result). */
public abstract class Screen {

    protected final Game game;

    public Screen(Game game) {
        this.game = game;
    }

    public void enter() { }

    public void exit() { }

    public abstract void update(float dt);

    public abstract void draw(Canvas c);

    /** @return true if back was consumed. */
    public boolean back() {
        return false;
    }
}
