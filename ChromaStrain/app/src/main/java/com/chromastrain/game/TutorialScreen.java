package com.chromastrain.game;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;

/**
 * A pressure-free room to learn each strain's controls: primary attack (gun),
 * secondary attack (melee — a different mechanic per faction), skill, gadget,
 * dose. Reuses World/Player unchanged; the World is built in "tutorial" mode
 * (see World's boolean constructor) so dummies never deal damage and nothing
 * ever transitions to defeat.
 */
public class TutorialScreen extends Screen {

    private static final int STEP_MOVE = 0;
    private static final int STEP_PRIMARY = 1;
    private static final int STEP_SECONDARY = 2;
    private static final int STEP_SKILL = 3;
    private static final int STEP_GADGET = 4;
    private static final int STEP_DOSE = 5;
    private static final int STEP_DONE = 6;

    private final World world;
    private int step;

    private float moveAccum, lastX, lastY;
    private int baseShots, baseMelee, baseCharge, baseCombo, baseSkill, baseGadget, baseDose;

    private final Input.Stick moveStick = new Input.Stick(70);
    private final Input.Stick aimStick = new Input.Stick(70);
    private final boolean[] tDown = new boolean[Input.MAX];
    private final float[] tX = new float[Input.MAX];
    private final float[] tY = new float[Input.MAX];
    private int meleeHoldPointer = -1;
    private int primaryHoldPointer = -1;

    private final float btnR = 54;
    private final float primaryR = 62;
    private float primaryX, primaryY;
    private float meleeX, meleeY, skillX, skillY, gadgetX, gadgetY, doseX, doseY;
    private final Ui.Btn skipBtn = new Ui.Btn();

    public TutorialScreen(Game game) {
        super(game);
        world = new World(game, true);
    }

    @Override
    public void enter() {
        game.sfx.music("menu");
        Player p = world.player;
        lastX = p.x;
        lastY = p.y;
        enterStep(STEP_MOVE);
    }

    private void enterStep(int s) {
        step = s;
        Player p = world.player;
        baseShots = p.shotsFired;
        baseMelee = p.meleeUses;
        baseCharge = p.chargeUses;
        baseCombo = p.comboUses;
        baseSkill = p.skillUses;
        baseGadget = p.gadgetUses;
        baseDose = p.doseUses;
        if (s > STEP_MOVE) game.sfx.play("ui_unlock", 0.8f, 1.05f);
    }

    private void layout() {
        float m = 24;
        primaryX = game.w - m - primaryR;
        primaryY = game.h - m - primaryR;

        float gap = 34;
        float pitch = btnR * 2.55f;
        float clusterX = primaryX - primaryR - gap - btnR;
        meleeX = clusterX;
        meleeY = game.h - m - btnR;
        skillX = clusterX;
        skillY = meleeY - pitch;
        gadgetX = clusterX - pitch;
        gadgetY = meleeY;
        doseX = clusterX - pitch;
        doseY = meleeY - pitch;
    }

    private boolean overButton(float x, float y) {
        return G.dist(x, y, primaryX, primaryY) < primaryR + 16
                || G.dist(x, y, meleeX, meleeY) < btnR + 16
                || G.dist(x, y, skillX, skillY) < btnR + 16
                || G.dist(x, y, gadgetX, gadgetY) < btnR + 16
                || G.dist(x, y, doseX, doseY) < btnR + 16
                || (x > game.w - 96 && y < 96);
    }

    @Override
    public void update(float dt) {
        layout();
        ArrayList<Input.Ev> evs = game.events;
        Player p = world.player;

        skipBtn.update(dt);
        if (skipBtn.tapped(evs)) {
            game.tapFeedback();
            game.switchTo(new HubScreen(game, 0));
            return;
        }

        if (step == STEP_DONE) {
            for (int i = 0; i < evs.size(); i++) {
                if (evs.get(i).type == 0) {
                    game.switchTo(new HubScreen(game, 0));
                    return;
                }
            }
        }

        for (int i = 0; i < evs.size(); i++) {
            Input.Ev e = evs.get(i);
            if (e.type != 0) continue;
            if (e.x > game.w - 96 && e.y < 96) continue; // reserved for skip button
            if (G.dist(e.x, e.y, primaryX, primaryY) < primaryR + 16) {
                primaryHoldPointer = e.id;
                continue;
            }
            if (G.dist(e.x, e.y, meleeX, meleeY) < btnR + 16) {
                p.meleeDown();
                meleeHoldPointer = e.id;
                continue;
            }
            if (G.dist(e.x, e.y, skillX, skillY) < btnR + 16) {
                p.trySkill();
                continue;
            }
            if (G.dist(e.x, e.y, gadgetX, gadgetY) < btnR + 16) {
                p.tryGadget();
                continue;
            }
            if (G.dist(e.x, e.y, doseX, doseY) < btnR + 16) {
                p.tryDose();
                continue;
            }
            if (e.x < game.w * 0.46f && !moveStick.active()) {
                moveStick.grab(e.id, e.x, e.y);
            } else if (e.x >= game.w * 0.46f && !aimStick.active() && !overButton(e.x, e.y)) {
                aimStick.grab(e.id, e.x, e.y);
            }
        }

        game.input.snapshot(tDown, tX, tY);
        moveStick.track(tDown, tX, tY);
        aimStick.track(tDown, tX, tY);

        if (meleeHoldPointer >= 0) {
            if (game.touchDown(meleeHoldPointer)) {
                p.meleeHeld(dt);
            } else {
                p.meleeRelease();
                meleeHoldPointer = -1;
            }
        }
        if (primaryHoldPointer >= 0 && !game.touchDown(primaryHoldPointer)) {
            primaryHoldPointer = -1;
        }

        float mvx = moveStick.dx, mvy = moveStick.dy;
        float mlen = G.len(mvx, mvy);
        if (mlen > 1f) {
            mvx /= mlen;
            mvy /= mlen;
        }
        if (mlen < 0.12f) mvx = mvy = 0;

        p.update(dt, mvx, mvy, aimStick.dx, aimStick.dy, primaryHoldPointer >= 0);
        world.update(dt);

        moveAccum += G.dist(p.x, p.y, lastX, lastY);
        lastX = p.x;
        lastY = p.y;

        checkStepCompletion();
    }

    private void checkStepCompletion() {
        Player p = world.player;
        switch (step) {
            case STEP_MOVE:
                if (moveAccum > 260) enterStep(STEP_PRIMARY);
                break;
            case STEP_PRIMARY:
                if (p.shotsFired - baseShots >= 6) enterStep(STEP_SECONDARY);
                break;
            case STEP_SECONDARY:
                if (p.faction == Strain.RED) {
                    if (p.chargeUses - baseCharge >= 1) enterStep(STEP_SKILL);
                } else if (p.faction == Strain.GREEN) {
                    if (p.comboUses - baseCombo >= 1) enterStep(STEP_SKILL);
                } else if (p.meleeUses - baseMelee >= 1) { // blue: any Deep Spike fire is enough
                    enterStep(STEP_SKILL);
                }
                break;
            case STEP_SKILL:
                if (p.skillUses - baseSkill >= 1) enterStep(STEP_GADGET);
                break;
            case STEP_GADGET:
                if (p.gadgetUses - baseGadget >= 1) enterStep(STEP_DOSE);
                break;
            case STEP_DOSE:
                if (p.doseUses - baseDose >= 1) enterStep(STEP_DONE);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean back() {
        game.switchTo(new HubScreen(game, 0));
        return true;
    }

    // ================================================================== draw

    @Override
    public void draw(Canvas c) {
        world.draw(c, game.w, game.h);
        drawHud(c);
    }

    private String secondaryText() {
        int f = world.player.faction;
        if (f == Strain.RED) {
            return "ATAQUE é seu SECUNDÁRIO — o Furybrand. Toque para um golpe rápido. "
                    + "SEGURE o botão e solte para um Golpe Carregado devastador.";
        } else if (f == Strain.GREEN) {
            return "ATAQUE é seu SECUNDÁRIO — as Whisperfangs, um golpe de curto alcance. Toque "
                    + "3 vezes seguidas: os dois primeiros cortam quem estiver no caminho do seu "
                    + "avanço, o 3º é o Corte Fantasma — avanço maior e crítico garantido.";
        } else {
            return "Você não usa corpo a corpo — ATAQUE dispara o Deep Spike, uma lança de "
                    + "gelo do seu Glacivore que atravessa tudo na linha e congela na hora. "
                    + "É seu SECUNDÁRIO: pesado, mas com cooldown próprio.";
        }
    }

    private String stepText() {
        Player p = world.player;
        switch (step) {
            case STEP_MOVE:
                return "Arraste o lado ESQUERDO da tela para se mover pela sala.";
            case STEP_PRIMARY:
                return "Arraste o lado DIREITO para mirar. SEGURE o botão ATIRAR para disparar "
                        + "seu ATAQUE PRIMÁRIO (a arma) na direção que você está mirando.";
            case STEP_SECONDARY:
                return secondaryText();
            case STEP_SKILL:
                return "Toque em HABILIDADE para usar " + Strain.SKILL_NAME[p.faction] + ".";
            case STEP_GADGET:
                return "Toque em GADGET para usar " + Strain.GADGET_NAME[p.faction] + ".";
            case STEP_DOSE:
                return "O medidor de Dose está cheio. Toque em DOSE para ativar "
                        + Strain.DOSE_NAME[p.faction] + ".";
            default:
                return "Treino completo! Toque em qualquer lugar para voltar ao Hangar.";
        }
    }

    private void drawHud(Canvas c) {
        Player p = world.player;
        int fcol = Strain.color(p.faction);

        if (moveStick.active()) drawStick(c, moveStick, fcol);
        if (aimStick.active()) drawStick(c, aimStick, Palette.INK_DIM);

        boolean holding = primaryHoldPointer >= 0;
        G.circle(c, primaryX, primaryY, primaryR, Palette.withAlpha(Palette.PANEL, 220));
        G.ring(c, primaryX, primaryY, primaryR, 3f, Palette.withAlpha(fcol, holding ? 255 : 150));
        Ui.icon(c, 17, primaryX, primaryY, primaryR * 0.95f, Palette.withAlpha(fcol, holding ? 255 : 190));
        if (holding) {
            float pulse = (float) Math.sin(game.time * 14f) * 0.5f + 0.5f;
            G.ring(c, primaryX, primaryY, primaryR + 6 + pulse * 5, 3f,
                    Palette.withAlpha(fcol, (int) (140 + 100 * pulse)));
        }

        int atkIcon = p.faction == Strain.BLUE ? 16 : 0;
        drawAbility(c, meleeX, meleeY, atkIcon, fcol, p.meleeCd, 1f / Strain.MELEE[p.faction][2], -1, true);
        drawAbility(c, skillX, skillY, 1, fcol, p.skillCd, Strain.SKILL_CD[p.faction] * p.cdMul, -1, true);
        drawAbility(c, gadgetX, gadgetY, 2, fcol, p.gadgetCd, Strain.GADGET_CD[p.faction] * p.cdMul, -1, true);
        boolean doseUp = p.doseReady();
        drawAbility(c, doseX, doseY, 3, doseUp ? Palette.DOSE : Palette.INK_DIM,
                0, 1, p.doseMeter / 100f, doseUp);

        if (p.faction == Strain.RED && p.meleeCharging) {
            float pulse = (float) Math.sin(game.time * 10f) * 0.5f + 0.5f;
            G.ring(c, meleeX, meleeY, btnR + 8 + pulse * 4, 3f,
                    Palette.withAlpha(Palette.GOLD, (int) (150 + 100 * pulse)));
        } else if (p.faction == Strain.GREEN) {
            int pip = p.comboIdx % 3;
            float dotY = meleeY - btnR - 16;
            for (int i = 0; i < 3; i++) {
                float dxp = meleeX + (i - 1) * 15;
                G.circle(c, dxp, dotY, 5, i < pip
                        ? Strain.color(p.faction) : Palette.withAlpha(Palette.INK_DIM, 90));
            }
        }

        G.textCB(c, "SALA DE TREINO", game.w / 2, 42, 26, Palette.INK);
        G.textC(c, Strain.FACTION[p.faction] + " — " + Strain.ROLE[p.faction], game.w / 2, 66, 14, fcol);

        float pw = Math.min(760, game.w * 0.66f);
        float px = game.w / 2 - pw / 2;
        float py = 84;
        Ui.panel(c, px, py, px + pw, py + 78);
        int stepNum = Math.min(step + 1, 6);
        G.textB(c, "PASSO " + stepNum + "/6", px + 20, py + 26, 13, Palette.withAlpha(fcol, 210));
        G.textWrap(c, stepText(), px + 20, py + 50, 16, Palette.INK, pw - 40, 21, G.FONT);

        for (int i = 0; i <= STEP_DONE; i++) {
            float dxp = px + 20 + i * 18;
            G.circle(c, dxp, py + 8, 3.5f, i <= step ? fcol : Palette.withAlpha(Palette.INK_DIM, 90));
        }

        skipBtn.set(game.w - 70, 46, 120, 48);
        skipBtn.label = "SAIR";
        skipBtn.textSize = 16;
        skipBtn.draw(c);
    }

    private void drawStick(Canvas c, Input.Stick s, int color) {
        G.ring(c, s.baseX, s.baseY, s.radius, 3f, Palette.withAlpha(color, 70));
        G.circle(c, s.baseX + s.dx * s.radius, s.baseY + s.dy * s.radius, 26,
                Palette.withAlpha(color, 130));
    }

    private void drawAbility(Canvas c, float x, float y, int icon, int color,
                             float cd, float cdMax, float meter, boolean ready) {
        G.circle(c, x, y, btnR, Palette.withAlpha(Palette.PANEL, 220));
        G.ring(c, x, y, btnR, 2.5f, Palette.withAlpha(color, ready ? 220 : 90));
        Ui.icon(c, icon, x, y, btnR * 0.95f, Palette.withAlpha(color, ready ? 255 : 120));
        if (cd > 0 && cdMax > 0) {
            G.cooldownSweep(c, x, y, btnR, G.clamp(cd / cdMax, 0, 1),
                    Palette.withAlpha(0xFF000000, 150));
            G.textCB(c, String.valueOf((int) Math.ceil(cd)), x, y + 7, 22, 0xFFFFFFFF);
        }
        if (meter >= 0) {
            G.P.setStyle(Paint.Style.STROKE);
            G.P.setStrokeWidth(4f);
            G.P.setColor(Palette.DOSE);
            c.drawArc(x - btnR + 3, y - btnR + 3, x + btnR - 3, y + btnR - 3,
                    -90, 360 * G.clamp(meter, 0, 1), false, G.P);
            G.P.setStyle(Paint.Style.FILL);
        }
    }
}
