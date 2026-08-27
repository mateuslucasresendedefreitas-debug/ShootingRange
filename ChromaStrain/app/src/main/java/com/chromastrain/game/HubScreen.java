package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/** Hub: OPERATIONS / LAB / CODEX / UPGRADES tabs + faction switching. */
public class HubScreen extends Screen {

    private static final int TAB_OPS = 0;
    private static final int TAB_LAB = 1;
    private static final int TAB_CODEX = 2;
    private static final int TAB_UP = 3;

    private static final String[] TAB_NAMES = {"OPERATIONS", "LAB", "CODEX", "UPGRADES"};
    private static final int[] TAB_ICONS = {13, 8, 7, 9};

    private int tab;
    private float t;

    private final float topH = 88;
    private final float botH = 92;

    // toast
    private String toast = "";
    private int toastColor;
    private float toastT;

    // ops
    private final Ui.Btn[] deployBtns = {new Ui.Btn(), new Ui.Btn(), new Ui.Btn()};

    // lab quiz
    private String[] chips = new String[6];
    private final boolean[] chipSel = new boolean[6];
    private final Ui.Btn confirmBtn = new Ui.Btn();
    private String labMsg = "";
    private int labMsgColor = Palette.INK_DIM;
    private int labFaction = -1;

    // codex
    private int codexSel;
    private float listScroll, bodyScroll;
    private int dragId = -1;
    private float dragLastY;
    private boolean dragOnList;
    private float bodyHeight = 400;

    // upgrades
    private final Ui.Btn[] upBtns = {new Ui.Btn(), new Ui.Btn(), new Ui.Btn(), new Ui.Btn()};
    private static final String[] UP_NAMES = {"VITALITY", "POWER", "REFLEX", "SYSTEMS"};
    private static final String[] UP_DESC = {
            "+12% max HP per level",
            "+8% all damage per level",
            "+5% move speed per level",
            "-6% cooldowns, +10% gadget power per level"};
    private static final int[] UP_COST = {80, 170, 300, 480, 720};

    public HubScreen(Game game, int tab) {
        super(game);
        this.tab = tab;
    }

    @Override
    public void enter() {
        game.sfx.music("menu");
        rollChips();
    }

    private void showToast(String s, int color) {
        toast = s;
        toastColor = color;
        toastT = 2.2f;
    }

    // ================================================================ update

    @Override
    public void update(float dt) {
        t += dt;
        if (toastT > 0) toastT -= dt;
        ArrayList<Input.Ev> evs = game.events;

        // tab bar
        for (int i = 0; i < evs.size(); i++) {
            Input.Ev e = evs.get(i);
            if (e.type == 0 && e.y > game.h - botH) {
                int nt = (int) (e.x / (game.w / 4f));
                if (nt >= 0 && nt < 4 && nt != tab) {
                    tab = nt;
                    game.tapFeedback();
                    if (tab == TAB_LAB && labFaction != game.save.faction) rollChips();
                }
            }
            // faction chips (top bar, left region)
            if (e.type == 0 && e.y < topH) {
                for (int f = 0; f < 3; f++) {
                    float cxx = 130 + f * 150;
                    if (Math.abs(e.x - cxx) < 70 && e.y > 8 && e.y < topH - 8) {
                        if (game.save.faction != f) {
                            game.save.faction = f;
                            game.save.flush();
                            game.sfx.play("ui_unlock", 0.7f, 1f);
                            game.haptic(15, 90);
                            if (tab == TAB_LAB) rollChips();
                        }
                    }
                }
            }
        }

        switch (tab) {
            case TAB_OPS: updateOps(dt, evs); break;
            case TAB_LAB: updateLab(dt, evs); break;
            case TAB_CODEX: updateCodex(dt, evs); break;
            default: updateUpgrades(dt, evs); break;
        }
    }

    private void updateOps(float dt, ArrayList<Input.Ev> evs) {
        int f = game.save.faction;
        for (int s = 0; s < 3; s++) {
            int opId = f * 3 + s;
            Ui.Btn b = deployBtns[s];
            b.update(dt);
            b.enabled = game.save.opUnlocked(opId);
            if (b.tapped(evs)) {
                if (b.enabled) {
                    game.tapFeedback();
                    game.switchTo(new RunScreen(game, opId));
                } else {
                    game.sfx.play("ui_deny", 0.7f, 1f);
                    showToast(Ops.lockText(opId), Palette.INK_DIM);
                }
            }
        }
    }

    private void rollChips() {
        int f = game.save.faction;
        labFaction = f;
        for (int i = 0; i < 6; i++) chipSel[i] = false;
        labMsg = "Pick TWO stimuli that make " + Strain.STRAIN[f]
                + " bloom — from two different types.";
        labMsgColor = Palette.INK_DIM;

        ArrayList<String> pool = new ArrayList<String>();
        // guarantee a valid pair: one from two random distinct categories
        int catA = G.rndi(0, 2);
        int catB = (catA + G.rndi(1, 2)) % 3;
        String[] a = Strain.GROWTH[f][catA];
        String[] bArr = Strain.GROWTH[f][catB];
        pool.add(a[G.rndi(0, a.length - 1)]);
        String pick2 = bArr[G.rndi(0, bArr.length - 1)];
        pool.add(pick2);
        // one extra from own faction (may be same category — a subtle trap tests the rule)
        int catC = G.rndi(0, 2);
        String[] cArr = Strain.GROWTH[f][catC];
        String extra = cArr[G.rndi(0, cArr.length - 1)];
        if (!pool.contains(extra)) pool.add(extra);
        // distractors from OTHER factions
        int guard = 0;
        while (pool.size() < 6 && guard++ < 60) {
            int of = (f + G.rndi(1, 2)) % 3;
            int oc = G.rndi(0, 2);
            String[] oArr = Strain.GROWTH[of][oc];
            String d = oArr[G.rndi(0, oArr.length - 1)];
            if (Strain.stimulusCategory(f, d) >= 0) continue; // must NOT belong to us
            if (!pool.contains(d)) pool.add(d);
        }
        // shuffle
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = G.rndi(0, i);
            String tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
        }
        for (int i = 0; i < 6; i++) {
            chips[i] = i < pool.size() ? pool.get(i) : "—";
        }
    }

    private void updateLab(float dt, ArrayList<Input.Ev> evs) {
        confirmBtn.update(dt);
        float gx = game.w * 0.40f, gy = topH + 60;
        float cw = (game.w * 0.56f) / 2 - 18, ch = 78;
        for (int i = 0; i < evs.size(); i++) {
            Input.Ev e = evs.get(i);
            if (e.type != 0) continue;
            for (int k = 0; k < 6; k++) {
                float cx = gx + (k % 2) * (cw + 14) + cw / 2;
                float cy = gy + (k / 2) * (ch + 12) + ch / 2;
                if (Math.abs(e.x - cx) < cw / 2 && Math.abs(e.y - cy) < ch / 2) {
                    int selCount = countSel();
                    if (chipSel[k]) {
                        chipSel[k] = false;
                        game.tapFeedback();
                    } else if (selCount < 2) {
                        chipSel[k] = true;
                        game.tapFeedback();
                    }
                }
            }
        }
        int f = game.save.faction;
        confirmBtn.enabled = countSel() == 2 && game.save.nodes >= 2;
        if (confirmBtn.tapped(evs)) {
            if (!confirmBtn.enabled) {
                if (game.save.nodes < 2) {
                    game.sfx.play("ui_deny", 0.8f, 1f);
                    labMsg = "Not enough raw nodes — harvest them in operations.";
                    labMsgColor = Palette.DANGER;
                }
                return;
            }
            game.save.nodes -= 2;
            String s1 = null, s2 = null;
            for (int k = 0; k < 6; k++) {
                if (chipSel[k]) {
                    if (s1 == null) s1 = chips[k];
                    else s2 = chips[k];
                }
            }
            int c1 = Strain.stimulusCategory(f, s1);
            int c2 = Strain.stimulusCategory(f, s2);
            if (c1 >= 0 && c2 >= 0 && c1 != c2) {
                game.save.doses[f]++;
                game.save.shards += 15;
                game.save.flush();
                game.sfx.play("craft_ok", 1f, 1f);
                game.haptic(40, 160);
                labMsg = "BLOOM! " + s1 + " (" + Strain.CATEGORY[c1] + ") + " + s2 + " ("
                        + Strain.CATEGORY[c2] + ") — +1 Stabilized Dose, +15 shards.";
                labMsgColor = Strain.color(f);
                rollChipsKeepMsg();
            } else {
                game.save.flush();
                game.sfx.play("craft_bad", 1f, 1f);
                if (c1 >= 0 && c2 >= 0) {
                    labMsg = "The culture stayed dormant: both stimuli are "
                            + Strain.CATEGORY[c1] + ". A strain needs at least TWO types. (-2 nodes)";
                } else {
                    String wrong = c1 < 0 ? s1 : s2;
                    int owner = Strain.stimulusOwner(wrong);
                    labMsg = wrong + " does not feed " + Strain.STRAIN[f] + " — it belongs to "
                            + (owner >= 0 ? Strain.STRAIN[owner] : "no strain") + ". (-2 nodes)";
                }
                labMsgColor = Palette.DANGER;
                rollChipsKeepMsg();
            }
        }
    }

    private void rollChipsKeepMsg() {
        String m = labMsg;
        int mc = labMsgColor;
        rollChips();
        labMsg = m;
        labMsgColor = mc;
    }

    private int countSel() {
        int n = 0;
        for (int i = 0; i < 6; i++) {
            if (chipSel[i]) n++;
        }
        return n;
    }

    private float dragTravel;

    private void updateCodex(float dt, ArrayList<Input.Ev> evs) {
        float listW = game.w * 0.36f;
        float rowH = 54;
        for (int i = 0; i < evs.size(); i++) {
            Input.Ev e = evs.get(i);
            if (e.type == 0 && e.y > topH && e.y < game.h - botH) {
                dragId = e.id;
                dragLastY = e.y;
                dragTravel = 0;
                dragOnList = e.x < listW;
            } else if (e.type == 1 && e.id == dragId) {
                // treat as a tap if the finger barely moved
                if (dragOnList && dragTravel < 14) {
                    int idx = (int) ((e.y - topH + listScroll) / rowH);
                    if (idx >= 0 && idx < CodexData.COUNT) {
                        if (codexSel != idx) {
                            codexSel = idx;
                            bodyScroll = 0;
                            game.tapFeedback();
                        }
                        if (!game.save.codexRead[idx]) {
                            game.save.codexRead[idx] = true;
                            game.save.shards += CodexData.READ_BONUS;
                            game.save.flush();
                            game.sfx.play("ui_unlock", 0.8f, 1.1f);
                            showToast("FIELD STUDY +" + CodexData.READ_BONUS + " SHARDS", Palette.SHARD);
                        }
                    }
                }
                dragId = -1;
            }
        }
        if (dragId >= 0 && game.touchDown(dragId)) {
            float ny = game.touchY(dragId);
            float d = ny - dragLastY;
            dragLastY = ny;
            dragTravel += Math.abs(d);
            if (dragOnList) {
                listScroll = G.clamp(listScroll - d, 0,
                        Math.max(0, CodexData.COUNT * rowH - (game.h - topH - botH)));
            } else {
                bodyScroll = G.clamp(bodyScroll - d, 0,
                        Math.max(0, bodyHeight - (game.h - topH - botH - 90)));
            }
        } else if (dragId >= 0) {
            dragId = -1;
        }
    }

    private void updateUpgrades(float dt, ArrayList<Input.Ev> evs) {
        for (int i = 0; i < 4; i++) {
            Ui.Btn b = upBtns[i];
            b.update(dt);
            int lvl = game.save.up[i];
            b.enabled = lvl < 5 && game.save.shards >= UP_COST[Math.min(lvl, 4)];
            if (b.tapped(evs)) {
                if (lvl >= 5) {
                    game.sfx.play("ui_deny", 0.7f, 1f);
                } else if (game.save.shards >= UP_COST[lvl]) {
                    game.save.shards -= UP_COST[lvl];
                    game.save.up[i]++;
                    game.save.flush();
                    game.sfx.play("ui_buy", 1f, 1f);
                    game.haptic(25, 120);
                    showToast(UP_NAMES[i] + " LEVEL " + game.save.up[i], Palette.GOLD);
                } else {
                    game.sfx.play("ui_deny", 0.7f, 1f);
                    showToast("NOT ENOUGH SHARDS", Palette.DANGER);
                }
            }
        }
    }

    @Override
    public boolean back() {
        game.switchTo(new TitleScreen(game));
        return true;
    }

    // ================================================================== draw

    @Override
    public void draw(Canvas c) {
        drawTopBar(c);
        switch (tab) {
            case TAB_OPS: drawOps(c); break;
            case TAB_LAB: drawLab(c); break;
            case TAB_CODEX: drawCodex(c); break;
            default: drawUpgrades(c); break;
        }
        drawTabBar(c);

        if (toastT > 0) {
            float a = G.clamp(toastT / 0.4f, 0, 1);
            G.rr(c, game.w / 2 - 260, game.h - botH - 64, game.w / 2 + 260, game.h - botH - 18, 12,
                    Palette.withAlpha(0xFF000000, (int) (190 * a)));
            G.textCB(c, toast, game.w / 2, game.h - botH - 33, 20,
                    Palette.withAlpha(toastColor, (int) (255 * a)));
        }
    }

    private void drawTopBar(Canvas c) {
        G.rr(c, 8, 8, game.w - 8, topH - 4, 16, Palette.PANEL);
        // faction chips
        for (int f = 0; f < 3; f++) {
            float cx = 130 + f * 150;
            boolean sel = game.save.faction == f;
            int col = Strain.color(f);
            if (sel) {
                G.rr(c, cx - 68, 14, cx + 68, topH - 12, 12, Palette.withAlpha(col, 46));
                G.rrStroke(c, cx - 68, 14, cx + 68, topH - 12, 12, 2.5f, col);
            }
            G.shard(c, cx - 44, topH / 2 + 1, 17, 25, 0.4f, Palette.withAlpha(col, sel ? 255 : 110), 0);
            G.textB(c, Strain.FACTION[f].split(" ")[0], cx - 22, topH / 2 - 3, 17,
                    sel ? Palette.INK : Palette.INK_DIM);
            G.textB(c, Strain.ROLE[f], cx - 22, topH / 2 + 15, 13,
                    sel ? col : Palette.withAlpha(Palette.INK_DIM, 130));
        }
        // currencies
        float rx = game.w - 30;
        G.textR(c, String.valueOf(game.save.shards), rx, topH / 2 + 7, 24, Palette.SHARD);
        G.shard(c, rx - G.textWidth(String.valueOf(game.save.shards), 24, G.FONT) - 22,
                topH / 2, 12, 17, 0.5f, Palette.SHARD, 0);
        float rx2 = rx - 150;
        G.textR(c, String.valueOf(game.save.nodes), rx2, topH / 2 + 7, 24, Palette.DOSE);
        G.shard(c, rx2 - G.textWidth(String.valueOf(game.save.nodes), 24, G.FONT) - 22,
                topH / 2, 12, 17, -0.4f, Palette.DOSE, 0);
        G.textR(c, "NODES", rx2 + 2, topH / 2 + 24, 11, Palette.INK_DIM);
        G.textR(c, "SHARDS", rx + 2, topH / 2 + 24, 11, Palette.INK_DIM);
    }

    private void drawTabBar(Canvas c) {
        G.rr(c, 8, game.h - botH + 4, game.w - 8, game.h - 8, 16, Palette.PANEL);
        for (int i = 0; i < 4; i++) {
            float cx = game.w / 8f + i * game.w / 4f;
            boolean sel = tab == i;
            int col = sel ? Strain.color(game.save.faction) : Palette.INK_DIM;
            if (sel) {
                G.rr(c, cx - 90, game.h - botH + 12, cx + 90, game.h - 16, 12,
                        Palette.withAlpha(col, 36));
            }
            Ui.icon(c, TAB_ICONS[i], cx - 52, game.h - botH / 2 - 2, 30, col);
            G.textB(c, TAB_NAMES[i], cx - 28, game.h - botH / 2 + 5, 17, col);
        }
    }

    private void drawOps(Canvas c) {
        int f = game.save.faction;
        float y0 = topH + 10;
        float cardH = (game.h - topH - botH - 40) / 3f;
        G.textB(c, Strain.FACTION[f] + " — " + Strain.CLASS_LINE[f], 24, y0 + 8, 16, Palette.INK_DIM);
        for (int s = 0; s < 3; s++) {
            int opId = f * 3 + s;
            float cy = y0 + 18 + s * (cardH + 6);
            boolean unlocked = game.save.opUnlocked(opId);
            int col = Strain.color(f);

            G.rr(c, 16, cy, game.w - 16, cy + cardH, 16, Palette.PANEL);
            G.rr(c, 16, cy, 26, cy + cardH, 6, Palette.withAlpha(col, unlocked ? 200 : 60));
            G.rrStroke(c, 16, cy, game.w - 16, cy + cardH, 16, 2f,
                    unlocked ? Palette.withAlpha(col, 90) : Palette.PANEL_EDGE);

            float tx = 46;
            G.textB(c, Ops.NAME[opId], tx, cy + 30, 24, unlocked ? Palette.INK : Palette.INK_DIM);
            float tagX = tx + G.textWidth(Ops.NAME[opId], 24, G.FONT_BOLD) + 16;
            G.rr(c, tagX, cy + 12, tagX + 84, cy + 36, 8, Palette.withAlpha(col, 40));
            G.textCB(c, Ops.SLOT_TAG[Ops.slot(opId)], tagX + 42, cy + 29, 14, col);

            if (unlocked) {
                G.text(c, Ops.MODIFIER[opId], tx, cy + 56, 15, Palette.INK_DIM);
                G.text(c, "BOSS: " + Ops.BOSS_NAME[opId], tx, cy + 78, 15,
                        Palette.withAlpha(Palette.DANGER, 200));
                if (game.save.best[opId] > 0) {
                    G.text(c, "BEST " + G.fmt(game.save.best[opId])
                            + (game.save.cleared[opId] ? "  ·  CLEARED" : ""), tx, cy + 100, 14, Palette.GOLD);
                }
            } else {
                Ui.icon(c, 14, tx + 12, cy + 66, 30, Palette.INK_DIM);
                G.text(c, Ops.lockText(opId), tx + 38, cy + 72, 15, Palette.INK_DIM);
            }

            Ui.Btn b = deployBtns[s];
            b.set(game.w - 110, cy + cardH / 2, 150, Math.min(64, cardH - 26));
            b.label = unlocked ? "DEPLOY" : "LOCKED";
            b.color = unlocked ? Palette.withAlpha(col, 40) : Palette.PANEL;
            b.edge = unlocked ? col : Palette.PANEL_EDGE;
            b.textSize = 22;
            b.draw(c);
        }
    }

    private void drawLab(Canvas c) {
        int f = game.save.faction;
        int col = Strain.color(f);
        float leftW = game.w * 0.36f;

        // left: strain + inventory
        Ui.panel(c, 16, topH + 10, leftW - 8, game.h - botH - 10);
        float lx = 34, ly = topH + 44;
        G.textB(c, "CULTIVATION LAB", lx, ly, 22, Palette.INK);
        ly += 30;
        G.textB(c, Strain.STRAIN[f].toUpperCase(), lx, ly, 19, col);
        ly += 24;
        ly += 20 * G.textWrap(c, Strain.TAGLINE[f], lx, ly, 14, Palette.INK_DIM,
                leftW - 70, 20, G.FONT);
        ly += 16;
        G.textB(c, "RAW NODES: " + game.save.nodes, lx, ly, 17, Palette.DOSE);
        ly += 24;
        G.textB(c, "STABILIZED DOSES (" + Strain.STRAIN[f] + "): " + game.save.doses[f],
                lx, ly, 17, Palette.DOSE);
        ly += 30;
        ly += 20 * G.textWrap(c,
                "A dose deploys you into your next operation with the Dose meter already "
                + "full. The valid stimuli are listed in this strain's codex profile.",
                lx, ly, 14, Palette.INK_DIM, leftW - 70, 20, G.FONT);

        // right: quiz
        float gx = game.w * 0.40f, gy = topH + 60;
        G.textB(c, "SELECT 2 STIMULI — COST: 2 NODES", gx, topH + 40, 18, Palette.INK);
        float cw = (game.w * 0.56f) / 2 - 18, ch = 78;
        for (int k = 0; k < 6; k++) {
            float cx = gx + (k % 2) * (cw + 14);
            float cy = gy + (k / 2) * (ch + 12);
            boolean sel = chipSel[k];
            G.rr(c, cx, cy, cx + cw, cy + ch, 14, sel ? Palette.withAlpha(col, 50) : Palette.PANEL);
            G.rrStroke(c, cx, cy, cx + cw, cy + ch, 14, sel ? 3f : 2f,
                    sel ? col : Palette.PANEL_EDGE);
            G.textCB(c, chips[k], cx + cw / 2, cy + ch / 2 + 8, 20, sel ? Palette.INK : Palette.INK_DIM);
        }
        confirmBtn.set(gx + (game.w * 0.56f) / 2 - 7, gy + 3 * (ch + 12) + 34, 300, 58);
        confirmBtn.label = "CULTIVATE";
        confirmBtn.color = Palette.withAlpha(col, 40);
        confirmBtn.edge = col;
        confirmBtn.draw(c);

        // message
        G.textWrap(c, labMsg, gx, gy + 3 * (ch + 12) + 84, 15, labMsgColor,
                game.w * 0.56f, 20, G.FONT);
    }

    private void drawCodex(Canvas c) {
        float listW = game.w * 0.36f;
        float rowH = 54;
        float top = topH;
        float bottom = game.h - botH;

        // list
        c.save();
        c.clipRect(0, top, listW, bottom);
        for (int i = 0; i < CodexData.COUNT; i++) {
            float ry = top + i * rowH - listScroll;
            if (ry < top - rowH || ry > bottom) continue;
            boolean sel = i == codexSel;
            int fcol = CodexData.FACTION[i] >= 0 ? Strain.color(CodexData.FACTION[i]) : Palette.SHARD;
            if (sel) {
                G.rr(c, 10, ry + 4, listW - 8, ry + rowH - 2, 10, Palette.withAlpha(fcol, 36));
            }
            G.circle(c, 30, ry + rowH / 2, 5,
                    game.save.codexRead[i] ? Palette.withAlpha(fcol, 220) : Palette.withAlpha(Palette.INK_DIM, 70));
            G.textB(c, CodexData.TITLE[i], 48, ry + rowH / 2 + 6, 15,
                    sel ? Palette.INK : Palette.INK_DIM);
        }
        c.restore();

        // detail
        Ui.panel(c, listW + 6, top + 8, game.w - 14, bottom - 8);
        c.save();
        c.clipRect(listW + 6, top + 8, game.w - 14, bottom - 8);
        float bx = listW + 30;
        int fcol = CodexData.FACTION[codexSel] >= 0
                ? Strain.color(CodexData.FACTION[codexSel]) : Palette.SHARD;
        G.textB(c, CodexData.TITLE[codexSel], bx, top + 48 - bodyScroll, 24, fcol);
        int lines = G.textWrap(c, CodexData.BODY[codexSel], bx, top + 86 - bodyScroll, 16,
                Palette.INK, game.w - bx - 40, 24, G.FONT);
        bodyHeight = 86 + lines * 24;
        c.restore();
        if (bodyHeight > bottom - top - 90) {
            G.textR(c, "· drag to scroll ·", game.w - 26, bottom - 20, 12,
                    Palette.withAlpha(Palette.INK_DIM, 120));
        }
    }

    private void drawUpgrades(Canvas c) {
        float y0 = topH + 14;
        float rowH = (game.h - topH - botH - 30) / 4f;
        for (int i = 0; i < 4; i++) {
            float ry = y0 + i * rowH;
            G.rr(c, 16, ry, game.w - 16, ry + rowH - 8, 16, Palette.PANEL);
            int lvl = game.save.up[i];
            G.textB(c, UP_NAMES[i], 40, ry + 34, 22, Palette.INK);
            G.text(c, UP_DESC[i], 40, ry + 60, 15, Palette.INK_DIM);
            // pips
            for (int p = 0; p < 5; p++) {
                float px = 40 + p * 34;
                G.rr(c, px, ry + 74, px + 26, ry + 86, 5,
                        p < lvl ? Palette.withAlpha(Palette.GOLD, 230)
                                : Palette.withAlpha(Palette.INK_DIM, 60));
            }
            Ui.Btn b = upBtns[i];
            b.set(game.w - 130, ry + rowH / 2 - 4, 190, Math.min(60, rowH - 26));
            if (lvl >= 5) {
                b.label = "MAX";
            } else {
                b.label = UP_COST[lvl] + " ◆";
            }
            b.color = Palette.withAlpha(Palette.SHARD, 26);
            b.edge = Palette.withAlpha(Palette.SHARD, 160);
            b.textSize = 21;
            b.draw(c);
        }
    }
}
