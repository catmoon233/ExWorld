package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.SkillPageClientState;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.SkillBarData;
import com.wan.gmmod.common.network.packet.ConfigureSkillPacket;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 技能配置界面。
 * <p>
 * 左侧显示 15 个（3 页 × 5 槽，每列一页，与 HUD 技能页划分一致）技能槽，
 * 右侧显示当前途径 / 序列已解锁的能力列表。
 * 操作：左键点击能力列表项选中该能力 → 左键点击槽位放入；左键点击已选中状态下的槽位放入，
 * 右键点击槽位清空该槽。指派 / 清空通过 {@link ConfigureSkillPacket} 发往服务端校验。
 */
public class SkillConfigScreen extends Screen {
    private static final ResourceLocation SLOT_TEX = GuimiMod.id("textures/gui/skill_slot.png");
    private static final ResourceLocation SLOT_DISABLED_TEX = GuimiMod.id("textures/gui/skill_slot_disabled.png");

    private static final int COLS = 3;
    private static final int ROWS = 5;
    private static final int SLOT = 20;
    private static final int ICON = 16;
    private static final int LIST_ROW_H = 20;

    private int gridX;
    private int gridY;
    private int listX;
    private int listY;
    /** 能力列表滚动偏移（行数） */
    private int scrollOffset;
    /** 能力列表可见行数 */
    private int visibleRows;

    private List<Ability> unlocked = List.of();
    /** 当前选中的待放置能力，null 表示未选中 */
    private Ability selected;

    public SkillConfigScreen() {
        super(Component.translatable("gui.guimi_mod.skill_config.title"));
    }

    @Override
    protected void init() {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        this.unlocked = player == null ? List.of() : SkillManager.getUnlockedAbilities(player);

        int gridW = COLS * SLOT;
        this.gridX = this.width / 2 - gridW - 30;
        this.gridY = 50;
        this.listX = this.width / 2 + 10;
        this.listY = 50;
        // 列表可见行数按屏幕高度计算，滚动偏移限制在合法范围内
        this.visibleRows = Math.max(1, (this.height - listY - 20) / LIST_ROW_H);
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, unlocked.size() - visibleRows));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return;
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("gui.guimi_mod.skill_config.slots"),
                gridX, gridY - 12, 0xC0C0C0);
        graphics.drawString(this.font, Component.translatable("gui.guimi_mod.skill_config.available"),
                listX, listY - 12, 0xC0C0C0);

        SkillBarData bar = player.getData(ModAttachments.SKILL_BAR);

        // 绘制 15 个技能槽（列主序：每列 = 一页）
        for (int i = 0; i < SkillBarData.SIZE; i++) {
            int sx = gridX + (i / ROWS) * SLOT;
            int sy = gridY + (i % ROWS) * SLOT;
            ResourceLocation id = bar.get(i);
            Ability ability = id == null ? null : findUnlocked(id);
            boolean available = ability != null;

            graphics.blit(available || ability == null ? SLOT_TEX : SLOT_DISABLED_TEX,
                    sx, sy, 0, 0, SLOT, SLOT, SLOT, SLOT);
            if (ability != null) {
                graphics.blit(ability.getIconTexture(), sx + 2, sy + 2, 0, 0, ICON, ICON, ICON, ICON);
            }
            // 悬停高亮
            if (isInBox(mouseX, mouseY, sx, sy, SLOT, SLOT)) {
                graphics.fill(sx, sy, sx + SLOT, sy + SLOT, 0x40FFFFFF);
                if (ability != null) {
                    graphics.renderTooltip(this.font, Component.translatable(ability.getNameKey()), mouseX, mouseY);
                }
            }
        }

        // 绘制可用能力列表（支持滚轮滚动，只绘制可见范围）
        int end = Math.min(unlocked.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < end; i++) {
            Ability ability = unlocked.get(i);
            int ry = listY + (i - scrollOffset) * LIST_ROW_H;
            boolean hover = isInBox(mouseX, mouseY, listX, ry, 140, ICON);
            if (ability == selected) {
                graphics.fill(listX - 2, ry - 2, listX + 142, ry + ICON + 2, 0x8033AAFF);
            } else if (hover) {
                graphics.fill(listX - 2, ry - 2, listX + 142, ry + ICON + 2, 0x40FFFFFF);
            }
            graphics.blit(ability.getIconTexture(), listX, ry, 0, 0, ICON, ICON, ICON, ICON);
            Component name = Component.translatable(ability.getNameKey());
            int color = ability.isActive() ? 0xFFFFFF : 0x999999;
            graphics.drawString(this.font, name, listX + ICON + 4, ry + 4, color);
        }
        // 滚动提示：列表超出可见区域时绘制上下箭头
        if (scrollOffset > 0) {
            graphics.drawCenteredString(this.font, Component.literal("▲"), listX + 70, listY - 10, 0xAAAAAA);
        }
        if (end < unlocked.size()) {
            graphics.drawCenteredString(this.font, Component.literal("▼"), listX + 70,
                    listY + visibleRows * LIST_ROW_H, 0xAAAAAA);
        }

        // 列脚标：页码（第 3 页为紧急技能页，红色标注）
        for (int p = 0; p < COLS; p++) {
            boolean emergency = p == SkillPageClientState.EMERGENCY_PAGE;
            Component label = emergency
                    ? Component.translatable("gui.guimi_mod.skill_config.page_emergency")
                    : Component.translatable("gui.guimi_mod.skill_config.page", p + 1);
            graphics.drawCenteredString(this.font, label,
                    gridX + p * SLOT + SLOT / 2, gridY + ROWS * SLOT + 2,
                    emergency ? 0xFF5555 : 0xAAAAAA);
        }

        // 提示文字
        Component hint = selected == null
                ? Component.translatable("gui.guimi_mod.skill_config.hint_select")
                : Component.translatable("gui.guimi_mod.skill_config.hint_place",
                        Component.translatable(selected.getNameKey()));
        graphics.drawString(this.font, hint, gridX, gridY + ROWS * SLOT + 14, 0xAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (unlocked.size() > visibleRows) {
            int maxOffset = unlocked.size() - visibleRows;
            this.scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击能力列表：选中（只命中可见范围内的行）
        int end = Math.min(unlocked.size(), scrollOffset + visibleRows);
        for (int i = scrollOffset; i < end; i++) {
            int ry = listY + (i - scrollOffset) * LIST_ROW_H;
            if (isInBox((int) mouseX, (int) mouseY, listX, ry, 140, ICON)) {
                this.selected = (this.selected == unlocked.get(i)) ? null : unlocked.get(i);
                return true;
            }
        }
        // 点击技能槽：左键放入 / 右键清空（列主序，与绘制一致）
        for (int i = 0; i < SkillBarData.SIZE; i++) {
            int sx = gridX + (i / ROWS) * SLOT;
            int sy = gridY + (i % ROWS) * SLOT;
            if (isInBox((int) mouseX, (int) mouseY, sx, sy, SLOT, SLOT)) {
                if (button == 1) {
                    PacketDistributor.sendToServer(new ConfigureSkillPacket(i, ""));
                } else if (selected != null) {
                    PacketDistributor.sendToServer(new ConfigureSkillPacket(i, selected.getId().toString()));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Ability findUnlocked(ResourceLocation id) {
        for (Ability ability : unlocked) {
            if (ability.getId().equals(id)) {
                return ability;
            }
        }
        return null;
    }

    private static boolean isInBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
