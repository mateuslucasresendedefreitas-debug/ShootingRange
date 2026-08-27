#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Procedural audio for Chroma Strain — every SFX and music loop is synthesized
(no external samples), keeping the whole art direction in one system and the
APK small. 22050 Hz mono 16-bit WAVs.
"""
import math
import os
import wave

import numpy as np

SR = 22050
HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(os.path.dirname(HERE), "app", "src", "main", "assets")
SFX = os.path.join(ASSETS, "sfx")
MUSIC = os.path.join(ASSETS, "music")

rng = np.random.default_rng(7)


def t_axis(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def env(n, a=0.005, d=0.15, curve=3.0):
    """attack/decay envelope over n samples"""
    t = np.arange(n) / SR
    e = np.minimum(t / max(a, 1e-5), 1.0)
    total = n / SR
    rel = np.clip((total - t) / max(d, 1e-5), 0, 1)
    return e * rel ** curve


def sine(f, dur, phase=0.0):
    t = t_axis(dur)
    if np.isscalar(f):
        return np.sin(2 * np.pi * f * t + phase)
    return np.sin(2 * np.pi * np.cumsum(f) / SR + phase)


def sweep(f0, f1, dur, shape=1.0):
    t = t_axis(dur)
    f = f0 * (f1 / f0) ** ((t / dur) ** shape)
    return sine(f, dur)


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def lowpass(x, alpha):
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += alpha * (x[i] - acc)
        y[i] = acc
    return y


def lp(x, cutoff):
    alpha = 1 - math.exp(-2 * math.pi * cutoff / SR)
    return lowpass(x, alpha)


def hp(x, cutoff):
    return x - lp(x, cutoff)


def dist(x, k=4.0):
    return np.tanh(x * k)


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[: len(p)] += p
    return out


def norm(x, gain=0.92):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * gain


def save(path, x, gain=0.92):
    x = norm(x, gain)
    data = (x * 32767).astype("<i2").tobytes()
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data)
    print("  %-28s %5.2fs  %6.1f KB" % (os.path.basename(path), len(x) / SR, len(data) / 1024))


def sfx(name, x, gain=0.92):
    save(os.path.join(SFX, name + ".wav"), x, gain)


# --------------------------------------------------------------------- sfx

def gen_sfx():
    # UI
    sfx("ui_tap", sine(1400, 0.05) * env(int(SR * 0.05), 0.001, 0.04) * 0.6)
    sfx("ui_back", sine(900, 0.06) * env(int(SR * 0.06), 0.001, 0.05) * 0.6)
    sfx("ui_buy", mix(sine(880, 0.16) * env(int(SR * 0.16), 0.002, 0.12),
                      sine(1320, 0.22) * env(int(SR * 0.22), 0.02, 0.16) * 0.7))
    d = mix(sine(220, 0.09), sine(233, 0.09)) * env(int(SR * 0.09), 0.002, 0.06)
    sfx("ui_deny", np.concatenate([d, np.zeros(int(SR * 0.03)), d]))
    sfx("ui_unlock", mix(sweep(700, 2100, 0.30) * env(int(SR * 0.30), 0.005, 0.24),
                         sine(2600, 0.3) * env(int(SR * 0.3), 0.08, 0.2) * 0.3))

    # weapons
    body = dist(lp(noise(0.16), 1800), 6) * env(int(SR * 0.16), 0.001, 0.12, 4)
    thump = sweep(170, 55, 0.16) * env(int(SR * 0.16), 0.001, 0.13)
    sfx("shot_red", mix(body, thump * 1.2))

    g = hp(lp(noise(0.07), 5200), 900) * env(int(SR * 0.07), 0.001, 0.05, 4)
    sfx("shot_green", mix(g, sine(1200, 0.05) * env(int(SR * 0.05), 0.001, 0.03) * 0.4), 0.8)

    laser = sweep(2100, 320, 0.30, 0.6) * env(int(SR * 0.30), 0.002, 0.26)
    shimmer = sine(3400, 0.3) * env(int(SR * 0.3), 0.01, 0.22) * 0.25
    sub = sweep(120, 60, 0.3) * env(int(SR * 0.3), 0.002, 0.2) * 0.8
    sfx("shot_blue", mix(laser, shimmer, sub))

    # melee
    wh = hp(lp(noise(0.22), 2600), 300)
    swf = wh * env(int(SR * 0.22), 0.03, 0.14)
    clank = dist(sine(320, 0.1), 8) * env(int(SR * 0.1), 0.001, 0.07)
    sfx("melee_red", mix(swf, np.concatenate([np.zeros(int(SR * 0.05)), clank])))

    s1 = hp(noise(0.08), 1500) * env(int(SR * 0.08), 0.005, 0.05)
    sfx("melee_green", np.concatenate([s1, np.zeros(int(SR * 0.02)), s1 * 0.8]), 0.75)

    zap = dist(sine(150, 0.12) + sine(97, 0.12) * 0.7, 6) * env(int(SR * 0.12), 0.001, 0.09)
    buzz = hp(noise(0.12), 3000) * env(int(SR * 0.12), 0.001, 0.06) * 0.5
    sfx("melee_blue", mix(zap, buzz))

    # impacts
    sfx("hit", mix(lp(noise(0.06), 3000) * env(int(SR * 0.06), 0.001, 0.045, 4),
                   sine(500, 0.05) * env(int(SR * 0.05), 0.001, 0.03) * 0.5), 0.7)
    sfx("crit", mix(sine(1750, 0.09) * env(int(SR * 0.09), 0.001, 0.07),
                    hp(noise(0.07), 2500) * env(int(SR * 0.07), 0.001, 0.05) * 0.7), 0.85)

    # crystal shatter kill
    parts = []
    for i in range(9):
        f = rng.uniform(1400, 4200)
        dur = rng.uniform(0.06, 0.16)
        off = int(SR * rng.uniform(0, 0.08))
        p = sine(f, dur) * env(int(SR * dur), 0.001, dur * 0.8, 2)
        parts.append(np.concatenate([np.zeros(off), p * rng.uniform(0.4, 1.0)]))
    parts.append(hp(noise(0.2), 1800) * env(int(SR * 0.2), 0.001, 0.15) * 0.7)
    sfx("kill", mix(*parts))

    boom = lp(noise(0.6), 320) * env(int(SR * 0.6), 0.002, 0.5, 2)
    crack = dist(lp(noise(0.1), 2400), 5) * env(int(SR * 0.1), 0.001, 0.06)
    subd = sweep(110, 40, 0.55) * env(int(SR * 0.55), 0.002, 0.45)
    sfx("explode", mix(boom * 1.2, crack, subd))

    sfx("hurt", mix(sweep(340, 120, 0.2) * env(int(SR * 0.2), 0.002, 0.16),
                    lp(noise(0.12), 900) * env(int(SR * 0.12), 0.001, 0.09) * 0.8))

    sfx("dash", hp(lp(noise(0.25), 3200), 500) * env(int(SR * 0.25), 0.04, 0.16))

    # skills
    sfx("skill_red", mix(lp(noise(0.7), 260) * env(int(SR * 0.7), 0.002, 0.6, 2) * 1.3,
                         sweep(150, 35, 0.7) * env(int(SR * 0.7), 0.001, 0.6),
                         dist(lp(noise(0.12), 2000), 4) * env(int(SR * 0.12), 0.001, 0.08)))
    sfx("skill_green", mix(sweep(500, 2400, 0.45, 0.7) * env(int(SR * 0.45), 0.02, 0.36),
                           hp(noise(0.45), 3500) * env(int(SR * 0.45), 0.05, 0.3) * 0.4), 0.8)
    arc = dist(sine(np.linspace(70, 90, int(SR * 0.5)), 0.5) * 0.8
               + hp(noise(0.5), 2000) * 0.6, 5) * env(int(SR * 0.5), 0.002, 0.4)
    ring = sine(1100, 0.5) * env(int(SR * 0.5), 0.05, 0.4) * 0.3
    sfx("skill_blue", mix(arc, ring))

    sfx("gadget", mix(dist(sine(240, 0.06), 6) * env(int(SR * 0.06), 0.001, 0.045),
                      np.concatenate([np.zeros(int(SR * 0.07)),
                                      sine(1500, 0.1) * env(int(SR * 0.1), 0.002, 0.08) * 0.6])))

    hiss = hp(noise(0.5), 4000) * env(int(SR * 0.5), 0.01, 0.4) * 0.5
    rise = sweep(300, 1900, 0.8, 0.8) * env(int(SR * 0.8), 0.05, 0.6) * 0.7
    beat = np.concatenate([sweep(140, 60, 0.12) * env(int(SR * 0.12), 0.001, 0.1),
                           np.zeros(int(SR * 0.18)),
                           sweep(140, 60, 0.12) * env(int(SR * 0.12), 0.001, 0.1)])
    sfx("dose", mix(hiss, rise, beat * 1.1))

    ice = []
    for i in range(6):
        f = rng.uniform(2400, 5200)
        off = int(SR * i * 0.035)
        p = sine(f, 0.12) * env(int(SR * 0.12), 0.001, 0.1, 2)
        ice.append(np.concatenate([np.zeros(off), p * 0.7]))
    ice.append(sweep(900, 2600, 0.35) * env(int(SR * 0.35), 0.01, 0.3) * 0.5)
    sfx("freeze", mix(*ice), 0.8)

    crackle = np.zeros(int(SR * 0.4))
    for i in range(26):
        off = int(rng.uniform(0, SR * 0.36))
        ln = int(SR * rng.uniform(0.004, 0.02))
        crackle[off:off + ln] += rng.uniform(-1, 1, ln) * rng.uniform(0.3, 1.0)
    sfx("burn", mix(lp(crackle, 3000), lp(noise(0.4), 500) * env(int(SR * 0.4), 0.02, 0.3) * 0.6), 0.7)

    horn = mix(dist(sine(196, 0.5) + sine(247, 0.5) * 0.8, 3) * env(int(SR * 0.5), 0.02, 0.4))
    sfx("wave", np.concatenate([horn * 0.8, horn]))

    growl = dist(sine(np.linspace(85, 55, int(SR * 1.1)), 1.1)
                 + sine(np.linspace(128, 82, int(SR * 1.1)), 1.1) * 0.7
                 + lp(noise(1.1), 400) * 0.8, 7) * env(int(SR * 1.1), 0.05, 0.9, 2)
    sfx("boss_roar", growl)

    n1 = sine(523, 0.18) * env(int(SR * 0.18), 0.004, 0.14)
    n2 = sine(659, 0.18) * env(int(SR * 0.18), 0.004, 0.14)
    n3 = mix(sine(784, 0.5), sine(1046, 0.5) * 0.6) * env(int(SR * 0.5), 0.004, 0.4)
    sfx("victory", np.concatenate([n1, n2, n3]))

    d1 = mix(sine(220, 0.5), sine(207, 0.5) * 0.8) * env(int(SR * 0.5), 0.02, 0.4)
    d2 = mix(sine(174, 0.9), sine(165, 0.9) * 0.8) * env(int(SR * 0.9), 0.02, 0.75)
    sfx("defeat", np.concatenate([d1, d2]))

    sfx("pickup", sine(1568, 0.09) * env(int(SR * 0.09), 0.001, 0.07), 0.6)
    sfx("craft_ok", mix(sine(660, 0.4) * env(int(SR * 0.4), 0.005, 0.32),
                        sine(990, 0.4) * env(int(SR * 0.4), 0.03, 0.3) * 0.7,
                        sine(1320, 0.45) * env(int(SR * 0.45), 0.06, 0.34) * 0.5))
    sfx("craft_bad", dist(mix(sine(140, 0.35), sine(146, 0.35)), 4) * env(int(SR * 0.35), 0.005, 0.28))
    sfx("heal", mix(sweep(400, 800, 0.5, 0.8), sweep(600, 1200, 0.5, 0.8) * 0.5)
        * env(int(SR * 0.5), 0.08, 0.36), 0.7)
    sfx("lock", dist(sine(180, 0.08), 8) * env(int(SR * 0.08), 0.001, 0.06))


# ------------------------------------------------------------------- music

def loopify(x, fade=0.6):
    """crossfades the tail into the head for a seamless loop"""
    n = int(SR * fade)
    head = x[:n].copy()
    tail = x[-n:].copy()
    ramp = np.linspace(0, 1, n)
    x = x[:-n]
    x[:n] = head * ramp + tail * (1 - ramp)
    return x


def pad_chord(freqs, dur, lfo=0.13, detune=1.004, amp=1.0):
    out = np.zeros(int(SR * dur))
    t = t_axis(dur)
    for f in freqs:
        w = np.sin(2 * np.pi * f * t) + np.sin(2 * np.pi * f * detune * t) * 0.7
        w *= (0.6 + 0.4 * np.sin(2 * np.pi * lfo * t + rng.uniform(0, 6)))
        out += w * amp
    return lp(out, 900)


def pulse_line(freq, dur, bpm, duty=0.5, amp=1.0, cutoff=700):
    out = np.zeros(int(SR * dur))
    step = 60.0 / bpm / 2
    n = int(dur / step)
    for i in range(n):
        ln = int(SR * step * duty)
        off = int(SR * i * step)
        seg = np.sin(2 * np.pi * freq * np.arange(ln) / SR)
        seg *= env(ln, 0.004, step * duty * 0.8, 2)
        out[off:off + ln] += seg * amp
    return lp(out, cutoff)


def sparkle(dur, dens, fmin, fmax, amp=0.3):
    out = np.zeros(int(SR * dur))
    for i in range(int(dur * dens)):
        off = int(rng.uniform(0, SR * (dur - 0.3)))
        f = rng.uniform(fmin, fmax)
        ln = int(SR * rng.uniform(0.15, 0.4))
        seg = np.sin(2 * np.pi * f * np.arange(ln) / SR) * env(ln, 0.01, 0.3, 2)
        seg = seg[: len(out) - off]  # clamp tails that would run past the loop
        out[off:off + len(seg)] += seg * amp * rng.uniform(0.4, 1)
    return out


def kick_pattern(dur, bpm, pattern, amp=1.0):
    out = np.zeros(int(SR * dur))
    beat = 60.0 / bpm
    n = int(dur / (beat / 2))
    k = int(SR * 0.14)
    kick = np.sin(2 * np.pi * np.cumsum(np.geomspace(130, 45, k)) / SR) * env(k, 0.001, 0.12)
    for i in range(n):
        if pattern[i % len(pattern)]:
            off = int(SR * i * beat / 2)
            end = min(len(out), off + k)
            out[off:end] += kick[: end - off] * amp
    return out


def gen_music():
    D = 16.0

    # menu — slow A-minor drift
    m = mix(pad_chord([110, 165, 220, 262], D, amp=0.5),
            sparkle(D, 1.2, 800, 1600, 0.16),
            pad_chord([55], D, lfo=0.07, amp=0.8))
    save(os.path.join(MUSIC, "menu.wav"), loopify(m), 0.55)

    # op0 red — driving, low, percussive (100 bpm)
    m = mix(pad_chord([73.4, 110, 146.8], D, lfo=0.2, amp=0.5),
            kick_pattern(D, 100, [1, 0, 0, 0, 1, 0, 1, 0], 1.1),
            pulse_line(146.8, D, 100, 0.28, 0.5, 500),
            dist(lp(noise(D), 180), 2) * 0.06)
    save(os.path.join(MUSIC, "op0.wav"), loopify(m), 0.6)

    # op1 green — airy, sparse, high sparkles
    m = mix(pad_chord([98, 147, 196, 247], D, lfo=0.1, amp=0.45),
            sparkle(D, 2.2, 1200, 3200, 0.14),
            pulse_line(392, D, 70, 0.12, 0.18, 1400))
    save(os.path.join(MUSIC, "op1.wav"), loopify(m), 0.55)

    # op2 blue — cold digital pad + beeps
    m = mix(pad_chord([87.3, 130.8, 174.6], D, lfo=0.16, amp=0.5),
            pulse_line(523, D, 90, 0.10, 0.16, 2200),
            pulse_line(65.4, D, 90, 0.5, 0.5, 300),
            sparkle(D, 1.0, 2000, 4200, 0.10))
    save(os.path.join(MUSIC, "op2.wav"), loopify(m), 0.6)

    # boss — tense ostinato
    m = mix(pad_chord([61.7, 92.5, 123.5], D, lfo=0.3, amp=0.6),
            kick_pattern(D, 120, [1, 0, 1, 0, 1, 0, 1, 1], 1.0),
            pulse_line(123.5, D, 120, 0.22, 0.6, 700),
            pulse_line(185, D, 120, 0.12, 0.3, 900),
            dist(lp(noise(D), 240), 2) * 0.05)
    save(os.path.join(MUSIC, "boss.wav"), loopify(m), 0.62)


if __name__ == "__main__":
    os.makedirs(SFX, exist_ok=True)
    os.makedirs(MUSIC, exist_ok=True)
    print("SFX:")
    gen_sfx()
    print("MUSIC:")
    gen_music()
    print("done")
