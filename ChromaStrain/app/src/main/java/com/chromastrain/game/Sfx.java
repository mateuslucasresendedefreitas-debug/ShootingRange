package com.chromastrain.game;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;
import java.util.HashMap;

/** SoundPool SFX + looping ambient music from (uncompressed) assets. */
public class Sfx {

    private final Context ctx;
    private SoundPool pool;
    private final HashMap<String, Integer> ids = new HashMap<String, Integer>();
    private MediaPlayer music;
    private String musicName = "";

    public boolean sfxOn = true;
    public boolean musicOn = true;

    private static final String[] NAMES = {
            "ui_tap", "ui_back", "ui_buy", "ui_deny", "ui_unlock",
            "shot_red", "shot_green", "shot_blue",
            "melee_red", "melee_green", "melee_blue",
            "hit", "crit", "kill", "explode",
            "hurt", "dash", "skill_red", "skill_green", "skill_blue",
            "gadget", "dose", "freeze", "burn",
            "wave", "boss_roar", "victory", "defeat",
            "pickup", "craft_ok", "craft_bad", "heal", "lock"
    };

    public Sfx(Context ctx) {
        this.ctx = ctx;
    }

    public void load() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(12).setAudioAttributes(attrs).build();
        for (int i = 0; i < NAMES.length; i++) {
            String n = NAMES[i];
            try {
                AssetFileDescriptor fd = ctx.getAssets().openFd("sfx/" + n + ".wav");
                ids.put(n, Integer.valueOf(pool.load(fd, 1)));
                fd.close();
            } catch (IOException ignored) {
                // missing sound: silently skipped
            }
        }
    }

    public void play(String name) {
        play(name, 1f, 1f);
    }

    public void play(String name, float vol, float pitch) {
        if (!sfxOn || pool == null) return;
        Integer id = ids.get(name);
        if (id == null) return;
        pool.play(id.intValue(), vol, vol, 1, 0, pitch);
    }

    /** Plays with slight random pitch variation — keeps repeated sfx organic. */
    public void playVar(String name, float vol) {
        play(name, vol, G.rnd(0.92f, 1.08f));
    }

    public void music(String name) {
        if (name.equals(musicName) && music != null) return;
        stopMusic();
        musicName = name;
        if (!musicOn || name.length() == 0) return;
        try {
            AssetFileDescriptor fd = ctx.getAssets().openFd("music/" + name + ".wav");
            music = new MediaPlayer();
            music.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            music.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            music.setLooping(true);
            music.setVolume(0.55f, 0.55f);
            music.prepare();
            music.start();
        } catch (Exception ignored) {
            music = null;
        }
    }

    public void setMusicOn(boolean on) {
        musicOn = on;
        if (!on) {
            stopMusic();
        } else if (musicName.length() > 0) {
            String n = musicName;
            musicName = "";
            music(n);
        }
    }

    public void stopMusic() {
        if (music != null) {
            try {
                music.stop();
                music.release();
            } catch (Exception ignored) { }
            music = null;
        }
        // keep musicName so setMusicOn(true) can resume
    }

    public void pause() {
        if (music != null) {
            try {
                music.pause();
            } catch (Exception ignored) { }
        }
    }

    public void resume() {
        if (music != null && musicOn) {
            try {
                music.start();
            } catch (Exception ignored) { }
        }
    }

    public void release() {
        stopMusic();
        if (pool != null) {
            pool.release();
            pool = null;
        }
    }
}
