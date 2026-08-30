package net.exmo.exworld.client.battle.screen;

import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Result presentation sharing the dark glass, cut corners and gold accents of the battle HUD.
 */
public final class BattleResultScreen extends Screen {
    private int selected = -1;

    public BattleResultScreen() {
        super(Component.translatable("screen.exworld.battle_result"));
    }

    @Override
    protected void init() {
        BattleSnapshot.PlayerResultView result = result();
        if (result == null) return;
        int count = result.candidates().size(), cardW = Math.min(150, Math.max(94, (width - 72) / Math.max(1, count))), gap = 10, total = count * cardW + Math.max(0, count - 1) * gap, start = (width - total) / 2;
        for (int i = 0; i < count; i++) {
            int index = i;
            String id = result.candidates().get(i);
            addRenderableWidget(Button.builder(Component.translatable(cardName(id)), button -> {
                selected = index;
                BattleClient.selectReward(id);
                rebuildWidgets();
            }).bounds(start + i * (cardW + gap), height / 2 + 60, cardW, 26).build());
        }
        boolean mayContinue = result.candidates().isEmpty() || result.selectedCandidate() != null || selected >= 0;
        addRenderableWidget(Button.builder(Component.translatable(result.confirmed() ? "screen.exworld.result_waiting" : "screen.exworld.result_continue"), button -> BattleClient.confirmResult()).bounds(width / 2 - 74, height - 48, 148, 25).build()).active = mayContinue && !result.confirmed();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }
    public void renderBackground2(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xE5080C12);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground2(g, mx, my, partial);
        var snapshot = BattleClient.snapshot();
        var result = snapshot == null ? null : snapshot.result();
        if (result == null) {
            super.render(g, mx, my, partial);
            return;
        }
        boolean victory = result.outcome().equals("VICTORY");
        int accent = victory ? 0xFFF2CB72 : 0xFFE6656D;
        int panelW = Math.min(700, width - 28), panelH = Math.min(390, height - 28), x = (width - panelW) / 2, y = (height - panelH) / 2;
        panel(g, x, y, panelW, panelH, accent);
        g.fill(x + 16, y + 15, x + 20, y + 70, accent);
        g.drawString(font, Component.translatable(victory ? "screen.exworld.result_victory" : "screen.exworld.result_defeat"), x + 32, y + 20, accent, true);
        g.drawString(font, Component.translatable(victory ? "screen.exworld.result_victory_subtitle" : "screen.exworld.result_defeat_subtitle"), x + 32, y + 42, 0xFFA8B3C0, false);
        var mine = result();
        if (mine != null) {
            int sy = y + 82;
            stat(g, x + 22, sy, Component.translatable("screen.exworld.stat_damage_dealt"), (int) mine.damageDealt(), 0xFFFF858B);
            stat(g, x + panelW / 4 + 12, sy, Component.translatable("screen.exworld.stat_damage_taken"), (int) mine.damageTaken(), 0xFFE6A16A);
            stat(g, x + panelW / 2 + 8, sy, Component.translatable("screen.exworld.stat_healing"), (int) mine.healing(), 0xFF77DDA2);
            stat(g, x + panelW * 3 / 4, sy, Component.translatable("screen.exworld.stat_cards"), mine.cardsUsed(), 0xFF80B8FF);
            g.drawCenteredString(font, Component.translatable("screen.exworld.result_summary", result.rounds(), result.durationTicks() / 20, result.downedUnits()), width / 2, sy + 46, 0xFFA8B3C0);
            if (victory) {
                g.drawCenteredString(font, Component.translatable("screen.exworld.result_rewards", mine.gold()), width / 2, height / 2 + 29, 0xFFF2CB72);
                if (!mine.candidates().isEmpty())
                    g.drawCenteredString(font, Component.translatable("screen.exworld.choose_reward"), width / 2, height / 2 + 45, 0xFFF5F7FA);
            } else
                g.drawCenteredString(font, Component.translatable("screen.exworld.defeat_no_rewards"), width / 2, height / 2 + 38, 0xFFE88C92);
        }
        super.render(g, mx, my, partial);
    }

    private void stat(GuiGraphics g, int x, int y, Component label, int value, int color) {
        g.drawString(font, label, x, y, 0xFF8F9BA8, false);
        g.pose().pushPose();
        g.pose().translate(x, y + 15, 0);
        g.pose().scale(1.35F, 1.35F, 1);
        g.drawString(font, Integer.toString(value), 0, 0, color, true);
        g.pose().popPose();
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x + 6, y, x + w - 6, y + h, 0xED111821);
        g.fill(x, y + 6, x + w, y + h - 6, 0xED111821);
        g.fill(x + 6, y, x + w - 6, y + 1, accent);
        g.fill(x + 6, y + h - 1, x + w - 6, y + h, 0x775D6C7C);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && minecraft != null) {
            minecraft.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private BattleSnapshot.PlayerResultView result() {
        var snapshot = BattleClient.snapshot();
        if (snapshot == null || snapshot.result() == null || minecraft == null || minecraft.player == null) return null;
        return snapshot.result().players().get(minecraft.player.getUUID());
    }

    private static String cardName(String id) {
        return id.startsWith("exworld:") ? "skill.exworld." + id.substring(8) : "spell." + id.replace(':', '.');
    }
}
