package com.wan.gmmod.client.gui;

import com.wan.gmmod.common.network.packet.StealChoicePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 盗火人「隔空盗窃」选择界面。
 * <p>
 * 列出目标身上的物品标签，点击某行回发 {@link StealChoicePacket} 完成偷取；
 * 滚轮可滚动，Esc 关闭（取消，技能仍消耗）。
 */
public class StealSelectScreen extends Screen {
    private static final int PANEL_W = 240;
    private static final int ROW_H = 16;
    private static final int VISIBLE = 10;
    private static final int TITLE_Y = 14;

    private final int targetId;
    private final List<String> labels;
    private int scroll;

    public StealSelectScreen(int targetId, List<String> labels) {
        super(Component.translatable("gui.guimi_mod.steal_select.title"));
        this.targetId = targetId;
        this.labels = labels;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, labels.size() - VISIBLE);
        this.scroll = Math.max(0, Math.min(maxScroll, this.scroll - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelH = TITLE_Y + Math.min(VISIBLE, labels.size()) * ROW_H + 18;
        int cx = (this.width - PANEL_W) / 2;
        int cy = (this.height - panelH) / 2;
        if (mouseX >= cx && mouseX < cx + PANEL_W) {
            int row = (int) ((mouseY - (cy + TITLE_Y)) / ROW_H);
            int global = scroll + row;
            if (row >= 0 && global >= 0 && global < labels.size()) {
                PacketDistributor.sendToServer(new StealChoicePacket(targetId, global));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        int rows = Math.min(VISIBLE, labels.size());
        int panelH = TITLE_Y + rows * ROW_H + 18;
        int cx = (this.width - PANEL_W) / 2;
        int cy = (this.height - panelH) / 2;
        graphics.fill(cx, cy, cx + PANEL_W, cy + panelH, 0xF0201818);
        graphics.fill(cx + 1, cy + 1, cx + PANEL_W - 1, cy + panelH - 1, 0xF02B2020);
        graphics.drawCenteredString(this.font, this.title, cx + PANEL_W / 2, cy + 4, 0xFFC9A45C);
        for (int i = 0; i < rows; i++) {
            int global = scroll + i;
            if (global >= labels.size()) break;
            int ly = cy + TITLE_Y + i * ROW_H;
            boolean hovered = mouseY >= ly && mouseY < ly + ROW_H
                    && mouseX >= cx && mouseX < cx + PANEL_W;
            if (hovered) {
                graphics.fill(cx + 3, ly + 1, cx + PANEL_W - 3, ly + ROW_H - 1, 0x35C9A45C);
            }
            String text = labels.get(global);
            if (this.font.width(text) > PANEL_W - 24) {
                text = this.font.plainSubstrByWidth(text, PANEL_W - 24);
            }
            graphics.drawString(this.font, text, cx + 12, ly + 4, hovered ? 0xFFE8C96A : 0xFFE0D6C0);
        }
    }
}