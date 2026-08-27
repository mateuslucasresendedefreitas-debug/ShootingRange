#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generates the Chroma Strain launcher icons procedurally (Pillow).

Look: a tri-colored Chromanite crystal cluster (red / green / blue strains)
glowing over a deep void background — the game's core fantasy in one glyph.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(os.path.dirname(HERE), "app", "src", "main", "res")

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

BG_TOP = (13, 17, 33)
BG_BOT = (5, 7, 14)
RED = (255, 66, 74)
GREEN = (66, 255, 148)
BLUE = (72, 156, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vertical_gradient(size, top, bot):
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        c = lerp(top, bot, y / max(1, size - 1))
        for x in range(size):
            px[x, y] = c
    return img


def rot(p, c, ang):
    s, co = math.sin(ang), math.cos(ang)
    x, y = p[0] - c[0], p[1] - c[1]
    return (c[0] + x * co - y * s, c[1] + x * s + y * co)


def shard(draw, cx, cy, w, h, ang, color, glow_layer):
    # crystal = elongated hexagon
    pts = [
        (cx, cy - h * 0.52),
        (cx + w * 0.42, cy - h * 0.18),
        (cx + w * 0.30, cy + h * 0.42),
        (cx, cy + h * 0.52),
        (cx - w * 0.30, cy + h * 0.42),
        (cx - w * 0.42, cy - h * 0.18),
    ]
    pts = [rot(p, (cx, cy), ang) for p in pts]
    hi = lerp(color, (255, 255, 255), 0.45)
    lo = lerp(color, (0, 0, 0), 0.35)
    draw.polygon(pts, fill=color)
    # faceted highlight (left face) and shadow (right face)
    left = [pts[0], pts[5], pts[4], pts[3]]
    right = [pts[0], pts[1], pts[2], pts[3]]
    draw.polygon(right, fill=lo)
    mid = [(0.6 * a + 0.4 * b for a, b in zip(pts[0], pts[3]))]
    draw.polygon([pts[0], pts[1], pts[3]], fill=color)
    draw.polygon([pts[0], pts[5], pts[3]], fill=hi)
    if glow_layer is not None:
        g = ImageDraw.Draw(glow_layer)
        g.polygon(pts, fill=color + (170,))


def draw_cluster(size, with_bg=True, scale=1.0):
    ss = 4  # supersample
    S = size * ss
    if with_bg:
        img = vertical_gradient(S, BG_TOP, BG_BOT).convert("RGBA")
    else:
        img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    cx, cy = S * 0.5, S * 0.56
    u = S * 0.20 * scale

    shard(d, cx - u * 1.05, cy + u * 0.18, u * 1.05, u * 2.0, -0.42, RED, glow)
    shard(d, cx + u * 1.05, cy + u * 0.22, u * 1.0, u * 1.85, 0.46, BLUE, glow)
    shard(d, cx, cy - u * 0.25, u * 1.15, u * 2.6, 0.0, GREEN, glow)

    glow = glow.filter(ImageFilter.GaussianBlur(S * 0.045))
    base = img
    img = Image.alpha_composite(glow, base.convert("RGBA")) if not with_bg else None
    if with_bg:
        bgimg = base
        out = Image.alpha_composite(bgimg.convert("RGBA"), glow)
        # redraw crystals crisply on top of glow
        d2 = ImageDraw.Draw(out)
        shard(d2, cx - u * 1.05, cy + u * 0.18, u * 1.05, u * 2.0, -0.42, RED, None)
        shard(d2, cx + u * 1.05, cy + u * 0.22, u * 1.0, u * 1.85, 0.46, BLUE, None)
        shard(d2, cx, cy - u * 0.25, u * 1.15, u * 2.6, 0.0, GREEN, None)
        img = out
    else:
        d2 = ImageDraw.Draw(img)
        shard(d2, cx - u * 1.05, cy + u * 0.18, u * 1.05, u * 2.0, -0.42, RED, None)
        shard(d2, cx + u * 1.05, cy + u * 0.22, u * 1.0, u * 1.85, 0.46, BLUE, None)
        shard(d2, cx, cy - u * 0.25, u * 1.15, u * 2.6, 0.0, GREEN, None)

    return img.resize((size, size), Image.LANCZOS)


def rounded_mask(size, radius_frac=0.22):
    m = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(m)
    r = int(size * radius_frac)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
    return m


def main():
    # legacy square-ish icons with rounded corners
    for dens, size in DENSITIES.items():
        icon = draw_cluster(size, with_bg=True)
        mask = rounded_mask(size)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(icon, (0, 0), mask)
        path = os.path.join(RES, "mipmap-%s" % dens)
        os.makedirs(path, exist_ok=True)
        out.save(os.path.join(path, "ic_launcher.png"))

    # adaptive icon layers (432px, content in inner 66% safe zone)
    xxx = os.path.join(RES, "mipmap-xxxhdpi")
    os.makedirs(xxx, exist_ok=True)
    vertical_gradient(432, BG_TOP, BG_BOT).save(os.path.join(xxx, "ic_launcher_bg.png"))
    fg = draw_cluster(432, with_bg=False, scale=0.62)
    fg.save(os.path.join(xxx, "ic_launcher_fg.png"))

    anydpi = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(anydpi, exist_ok=True)
    with open(os.path.join(anydpi, "ic_launcher.xml"), "w") as f:
        f.write(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@mipmap/ic_launcher_bg" />\n'
            '    <foreground android:drawable="@mipmap/ic_launcher_fg" />\n'
            "</adaptive-icon>\n"
        )
    print("icons written")


if __name__ == "__main__":
    main()
