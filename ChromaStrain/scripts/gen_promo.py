#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Promo renders for docs: recreates the game's draw routines (same palette,
same shard/glow shapes) with Pillow to produce representative stills of the
run screen, the hub and a wide banner.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
MEDIA = os.path.join(os.path.dirname(HERE), "docs", "media")

VOID_TOP = (11, 14, 26)
VOID_BOT = (4, 6, 12)
GRID = (120, 180, 255, 9)
PANEL = (13, 18, 32, 226)
PANEL_EDGE = (130, 200, 255, 90)
INK = (226, 236, 248)
INK_DIM = (140, 152, 176)
GOLD = (255, 205, 96)
DANGER = (255, 82, 92)
RED = (255, 74, 82)
RED_DARK = (122, 22, 30)
GREEN = (84, 255, 158)
GREEN_DARK = (16, 106, 62)
BLUE = (92, 168, 255)
BLUE_DARK = (24, 62, 128)
DOSE = (214, 120, 255)
SHARD = (150, 232, 255)
HP = (255, 96, 110)


def font(size, bold=True):
    for name in (["DejaVuSans-Bold.ttf"] if bold else ["DejaVuSans.ttf"]):
        for base in ("/usr/share/fonts/truetype/dejavu/",):
            p = base + name
            if os.path.exists(p):
                return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


PANEL_RGB = (13, 18, 32)
VOID_RGB = (7, 9, 17)


def on(base, col, alpha):
    """Pre-blends col at alpha over an assumed base — this Pillow build does
    not composite RGBA fills for shape primitives."""
    t = alpha / 255.0
    return tuple(int(base[i] + (col[i] - base[i]) * t) for i in range(3)) + (255,)


def bg(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        c = mix(VOID_TOP, VOID_BOT, y / max(1, h - 1))
        for x in range(w):
            px[x, y] = c
    return img.convert("RGBA")


def glow_layer(size):
    return Image.new("RGBA", size, (0, 0, 0, 0))


def glow(layer, x, y, r, color, alpha=90):
    d = ImageDraw.Draw(layer)
    d.ellipse([x - r, y - r, x + r, y + r], fill=color + (alpha,))


def shard_pts(x, y, w, h, ang):
    px = [0, 0.42, 0.30, 0, -0.30, -0.42]
    py = [-0.52, -0.18, 0.42, 0.52, 0.42, -0.18]
    ca, sa = math.cos(ang), math.sin(ang)
    return [(x + px[i] * w * ca - py[i] * h * sa,
             y + px[i] * w * sa + py[i] * h * ca) for i in range(6)]


def shard(d, x, y, w, h, ang, fill, edge=None, ew=3):
    pts = shard_pts(x, y, w, h, ang)
    d.polygon(pts, fill=fill)
    if edge:
        d.line(pts + [pts[0]], fill=edge, width=ew, joint="curve")


def rr(d, box, rad, fill=None, outline=None, width=2):
    d.rounded_rectangle(box, radius=rad, fill=fill, outline=outline, width=width)


def text(d, s, x, y, size, color, bold=True, anchor="la"):
    d.text((x, y), s, font=font(size, bold), fill=color, anchor=anchor)


def compose(base, gl):
    gl = gl.filter(ImageFilter.GaussianBlur(14))
    return Image.alpha_composite(base, gl)


# --------------------------------------------------------------- run screen

def gen_run():
    W, H = 1600, 720
    img = bg(W, H)
    d = ImageDraw.Draw(img, "RGBA")
    # grid (camera offset)
    grid_col = on(VOID_RGB, (120, 180, 255), 22)
    for gx in range(-40, W, 100):
        d.line([(gx, 0), (gx, H)], fill=grid_col, width=1)
    for gy in range(-30, H, 100):
        d.line([(0, gy), (W, gy)], fill=grid_col, width=1)
    # arena border hint
    d.line([(60, 0), (60, H)], fill=on(VOID_RGB, GREEN, 150), width=6)

    gl = glow_layer((W, H))
    # pillars
    for (px, py, pr, ang) in [(300, 560, 62, 1.1), (1290, 170, 52, 2.3)]:
        glow(gl, px, py, pr * 2, GREEN, 26)
        shard(d, px, py, pr * 1.6, pr * 2.1, ang, mix(VOID_TOP, GREEN, 0.22), GREEN + (120,))

    # fire zone (red gadget visual borrowed for spice) — use green decoy flash instead
    # trap: enemies
    def husk(x, y, ang, hurt=0.0):
        glow(gl, x, y, 44, GREEN, 30)
        body = mix(GREEN_DARK, GREEN, 0.25 + hurt)
        shard(d, x, y, 31, 42, ang, body + (255,), GREEN + (255,), 3)

    def spitter(x, y, wind=0.0):
        glow(gl, x, y, 52, GREEN, 36)
        d.ellipse([x - 24, y - 24, x + 24, y + 24], fill=mix(GREEN_DARK, GREEN, 0.25) + (255,),
                  outline=GREEN + (255,), width=3)
        d.ellipse([x - 9, y - 9, x + 9, y + 9], fill=(255, 255, 255, 128))
        if wind:
            d.ellipse([x - 34, y - 34, x + 34, y + 34], outline=(255, 255, 255, 130), width=2)

    def brute(x, y):
        glow(gl, x, y, 76, GREEN, 30)
        shard(d, x, y, 76, 76, 0.78, mix(GREEN_DARK, GREEN, 0.25) + (255,), GREEN + (255,), 4)
        shard(d, x, y, 44, 44, -0.3, GREEN_DARK + (255,), GREEN + (255,), 3)

    husk(1050, 250, 2.4)
    husk(1180, 420, 2.9, 0.5)
    husk(880, 160, 2.1)
    spitter(1360, 300, 1)
    brute(1240, 585)
    # elite ring on one husk
    d.ellipse([1180 - 38, 420 - 38, 1180 + 38, 420 + 38], outline=GOLD + (200,), width=3)

    # decoy
    glow(gl, 640, 540, 70, GREEN, 40)
    shard(d, 640, 540, 38, 52, 0, GREEN_DARK + (160,), GREEN + (190,))

    # player (green phantom) with dose aura
    px, py = 620, 360
    glow(gl, px, py, 90, DOSE, 46)
    glow(gl, px, py, 60, GREEN, 40)
    shard(d, px, py, 39, 52, math.pi / 2 + 0.35, mix(GREEN_DARK, GREEN, 0.45) + (255,), GREEN + (255,), 3)
    d.ellipse([px - 11, py - 11, px + 11, py + 11], fill=(255, 255, 255, 218))
    d.line([(px + 12, py + 4), (px + 40, py + 12)], fill=GREEN + (255,), width=6)

    # bullets + trails
    for i, (bx, by) in enumerate([(760, 385), (860, 402), (960, 420)]):
        glow(gl, bx, by, 26, GREEN, 70)
        d.ellipse([bx - 5, by - 5, bx + 5, by + 5], fill=(255, 255, 255, 255))
    # enemy bolt
    glow(gl, 1150, 330, 24, GREEN, 60)
    d.ellipse([1150 - 6, 330 - 6, 1150 + 6, 330 + 6], fill=GREEN + (255,))

    # particles
    import random
    rnd = random.Random(4)
    for _ in range(26):
        sx, sy = 1180 + rnd.uniform(-70, 70), 420 + rnd.uniform(-60, 60)
        r = rnd.uniform(1.5, 4)
        d.ellipse([sx - r, sy - r, sx + r, sy + r], fill=GREEN + (rnd.randint(60, 200),))

    # damage numbers
    text(d, "312", 1170, 356, 24, (255, 255, 255, 240), anchor="mm")
    text(d, "561", 1044, 118, 30, GOLD + (255,), anchor="mm")
    text(d, "HEMORRHAGE", 1180, 492, 15, GREEN + (230,), anchor="mm")

    img = compose(img, gl)
    d = ImageDraw.Draw(img, "RGBA")

    # ------------- HUD
    # hp + dose
    rr(d, [24, 24, 324, 46], 8, fill=on(VOID_RGB, (60, 16, 24), 140))
    rr(d, [24, 24, 24 + 300 * 0.72, 46], 8, fill=HP + (255,))
    text(d, "1224 / 1700", 32, 29, 14, (255, 255, 255, 255))
    rr(d, [24, 54, 234, 66], 5, fill=on(VOID_RGB, DOSE, 50))
    rr(d, [24, 54, 214, 66], 5, fill=DOSE + (255,))
    text(d, "DOSED 5s", 244, 52, 14, DOSE + (255,))
    text(d, "CLOAKED", 24, 76, 15, GREEN + (255,))

    # wave + banner
    text(d, "WAVE 3 / 5", 800, 26, 19, INK_DIM + (255,), anchor="ma")
    text(d, "TEMPLE OF THE HOLLOW SIGN", 780, 216, 40, GREEN + (235,), anchor="mm")

    # score + pause
    text(d, "8.4k", 1470, 30, 25, INK + (255,), anchor="ra")
    rr(d, [1522, 18, 1578, 74], 14, fill=PANEL)
    d.line([(1543, 32), (1543, 60)], fill=INK_DIM + (255,), width=5)
    d.line([(1557, 32), (1557, 60)], fill=INK_DIM + (255,), width=5)

    # sticks
    d.ellipse([150, 480, 290, 620], outline=GREEN + (80,), width=3)
    d.ellipse([236, 524, 288, 576], fill=GREEN + (120,))

    # ability buttons
    def ability(x, y, ready, icon, meter=None):
        d.ellipse([x - 46, y - 46, x + 46, y + 46], fill=(13, 18, 32, 220))
        col = (GREEN if ready else INK_DIM) + ((230 if ready else 110),)
        d.ellipse([x - 46, y - 46, x + 46, y + 46], outline=col, width=3)
        icon(x, y, col)
        if meter is not None:
            d.arc([x - 43, y - 43, x + 43, y + 43], -90, -90 + 360 * meter, fill=DOSE + (255,), width=4)

    def ic_blade(x, y, col):
        d.line([(x - 16, y + 16), (x + 16, y - 16)], fill=col, width=5)
        d.line([(x - 16, y - 16), (x + 2, y + 2)], fill=col, width=5)

    def ic_bolt(x, y, col):
        d.polygon([(x + 5, y - 20), (x - 11, y + 3), (x + 1, y + 3), (x - 5, y + 20),
                   (x + 12, y - 4), (x + 1, y - 4)], fill=col)

    def ic_flask(x, y, col):
        d.line([(x - 4, y - 17), (x - 4, y - 4)], fill=col, width=4)
        d.line([(x + 4, y - 17), (x + 4, y - 4)], fill=col, width=4)
        d.polygon([(x - 4, y - 4), (x - 14, y + 14), (x + 14, y + 14), (x + 4, y - 4)], outline=col, width=3)

    def ic_syringe(x, y, col):
        d.rectangle([x - 5, y - 12, x + 5, y + 6], outline=col, width=3)
        d.line([(x, y + 6), (x, y + 18)], fill=col, width=3)
        d.line([(x - 9, y - 12), (x + 9, y - 12)], fill=col, width=3)

    ability(1554, 648, True, ic_blade)
    ability(1439, 648, False, ic_bolt)   # skill on cooldown
    d.pieslice([1439 - 46, 648 - 46, 1439 + 46, 648 + 46], -90, 40,
               fill=on(PANEL_RGB, (0, 0, 0), 150))
    text(d, "6", 1439, 648, 22, (255, 255, 255, 255), anchor="mm")
    ability(1554, 533, True, ic_flask)
    ability(1462, 556, True, ic_syringe, meter=1.0)

    os.makedirs(MEDIA, exist_ok=True)
    img.convert("RGB").save(os.path.join(MEDIA, "screen_run.png"))
    print("screen_run.png")


# ---------------------------------------------------------------- hub screen

def gen_hub():
    W, H = 1600, 720
    img = bg(W, H)
    d = ImageDraw.Draw(img, "RGBA")
    gl = glow_layer((W, H))

    # top bar
    rr(d, [8, 8, W - 8, 84], 16, fill=PANEL_RGB + (255,))
    chips = [("CRIMSON", "VANGUARD", RED), ("VERDANT", "PHANTOM", GREEN), ("NAVY", "SAVANT", BLUE)]
    for i, (nm, role, col) in enumerate(chips):
        cx = 130 + i * 150
        if i == 1:
            rr(d, [cx - 68, 14, cx + 68, 76], 12, fill=on(PANEL_RGB, col, 46),
               outline=col + (255,), width=3)
        shard(d, cx - 44, 46, 17, 25, 0.4,
              (col if i == 1 else on(PANEL_RGB, col, 110)) + ((255,) if i == 1 else ()))
        text(d, nm, cx - 22, 32, 15, (INK if i == 1 else INK_DIM) + (255,))
        text(d, role, cx - 22, 52, 12, (col if i == 1 else INK_DIM) + (255,))
    text(d, "417", 1568, 34, 22, SHARD + (255,), anchor="ra")
    text(d, "SHARDS", 1570, 62, 10, INK_DIM + (255,), anchor="ra")
    shard(d, 1500, 45, 12, 17, 0.5, SHARD + (255,))
    text(d, "9", 1420, 34, 22, DOSE + (255,), anchor="ra")
    text(d, "NODES", 1422, 62, 10, INK_DIM + (255,), anchor="ra")
    shard(d, 1372, 45, 12, 17, -0.4, DOSE + (255,))

    text(d, "VERDANT HERD — Agility · Precision · Willpower", 24, 96, 15, INK_DIM + (255,))

    ops = [
        ("GRAVENIGHT SPIRE", "TRIALS", "FLICKERING REALITY — enemy behavior shifts every wave",
         "BOSS: THYRAL, GHOST-KISSED", "BEST 6.2k  ·  CLEARED", True),
        ("TEMPLE OF THE HOLLOW SIGN", "RAID", "HOLLOW SIGN — distant enemies fade from sight",
         "BOSS: THE ECHO PRIESTESS", "BEST 8.4k", True),
        ("CLOTHWALKER", "HUNT", "", "Clear TEMPLE OF THE HOLLOW SIGN to unlock", "", False),
    ]
    y0 = 118
    cardH = 152
    for i, (nm, tag, mod, boss, best, unlocked) in enumerate(ops):
        cy = y0 + i * (cardH + 8)
        rr(d, [16, cy, W - 16, cy + cardH], 16, fill=PANEL_RGB + (255,),
           outline=on(VOID_RGB, GREEN, 90) if unlocked else on(VOID_RGB, (130, 200, 255), 90),
           width=2)
        rr(d, [16, cy, 26, cy + cardH], 6, fill=on(PANEL_RGB, GREEN, 200 if unlocked else 60))
        text(d, nm, 46, cy + 16, 22, (INK if unlocked else INK_DIM) + (255,))
        tw = d.textlength(nm, font=font(22))
        rr(d, [56 + tw, cy + 12, 140 + tw, cy + 38], 8, fill=on(PANEL_RGB, GREEN, 40))
        text(d, tag, 98 + tw, cy + 18, 13, GREEN + (255,), anchor="ma")
        if unlocked:
            text(d, mod, 46, cy + 52, 14, INK_DIM + (255,), bold=False)
            text(d, boss, 46, cy + 76, 14, DANGER + (220,))
            if best:
                text(d, best, 46, cy + 100, 13, GOLD + (255,))
        else:
            # little padlock
            d.rectangle([48, cy + 66, 70, cy + 82], outline=INK_DIM + (255,), width=3)
            d.arc([52, cy + 52, 66, cy + 70], 180, 360, fill=INK_DIM + (255,), width=3)
            text(d, boss, 84, cy + 62, 14, INK_DIM + (255,), bold=False)
        bx, by = W - 110, cy + cardH / 2
        rr(d, [bx - 75, by - 28, bx + 75, by + 28], 13,
           fill=on(PANEL_RGB, GREEN, 40) if unlocked else PANEL_RGB + (255,),
           outline=(GREEN + (255,)) if unlocked else on(PANEL_RGB, (130, 200, 255), 120), width=2)
        text(d, "DEPLOY" if unlocked else "LOCKED", bx, by - 10, 19,
             (INK if unlocked else INK_DIM) + (255,), anchor="ma")

    # tab bar
    rr(d, [8, H - 88, W - 8, H - 8], 16, fill=PANEL_RGB + (255,))
    tabs = ["OPERATIONS", "LAB", "CODEX", "UPGRADES"]
    for i, tname in enumerate(tabs):
        cx = W / 8 + i * W / 4
        col = GREEN if i == 0 else INK_DIM
        if i == 0:
            rr(d, [cx - 100, H - 80, cx + 100, H - 16], 12, fill=on(PANEL_RGB, GREEN, 36))
        text(d, tname, cx, H - 58, 16, col + (255,), anchor="ma")

    img = compose(img, gl)
    img.convert("RGB").save(os.path.join(MEDIA, "screen_hub.png"))
    print("screen_hub.png")


# ------------------------------------------------------------------- banner

def gen_banner():
    W, H = 1600, 560
    img = bg(W, H)
    d = ImageDraw.Draw(img, "RGBA")
    gl = glow_layer((W, H))

    import random
    rnd = random.Random(11)
    for _ in range(26):
        x, y = rnd.uniform(0, W), rnd.uniform(0, H)
        col = [RED, GREEN, BLUE][rnd.randint(0, 2)]
        shard(d, x, y, rnd.uniform(8, 22), rnd.uniform(14, 34), rnd.uniform(0, 6),
              on(VOID_RGB, col, 26))

    cx, cy = W * 0.26, H * 0.52
    glow(gl, cx, cy, 250, GREEN, 30)
    glow(gl, cx - 90, cy + 40, 140, RED, 34)
    glow(gl, cx + 90, cy + 44, 140, BLUE, 34)
    shard(d, cx - 92, cy + 34, 82, 150, -0.42, mix(RED_DARK, RED, 0.5) + (255,), RED + (255,), 4)
    shard(d, cx + 92, cy + 38, 78, 140, 0.46, mix(BLUE_DARK, BLUE, 0.5) + (255,), BLUE + (255,), 4)
    shard(d, cx, cy - 16, 92, 198, 0.0, mix(GREEN_DARK, GREEN, 0.5) + (255,), GREEN + (255,), 4)
    d.ellipse([cx - 15, cy - 22, cx + 15, cy + 8], fill=(255, 255, 255, 235))

    text(d, "CHROMA", W * 0.47, H * 0.30, 110, INK + (255,))
    text(d, "STRAIN", W * 0.47, H * 0.30 + 118, 110, GREEN + (255,))
    text(d, "a Chromanite codex game — 3 strains · 9 operations · 9 bosses",
         W * 0.47, H * 0.30 + 244, 26, INK_DIM + (255,), bold=False)

    img = compose(img, gl)
    img.convert("RGB").save(os.path.join(MEDIA, "banner.png"))
    print("banner.png")


if __name__ == "__main__":
    gen_run()
    gen_hub()
    gen_banner()
