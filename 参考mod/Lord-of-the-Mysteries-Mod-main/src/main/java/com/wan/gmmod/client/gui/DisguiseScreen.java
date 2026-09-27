package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.render.DisguiseRenderHandler;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.DisguiseData;
import com.wan.gmmod.common.capability.data.DisguiseUnlocks;
import com.wan.gmmod.common.network.packet.SelectDisguisePacket;
import com.wan.gmmod.content.disguise.HumanoidDisguises;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 变形选择界面（无面人 · 序列 6）。
 * <p>
 * 分为两个标签页：
 * <ul>
 *     <li><b>记忆库</b>：列出已解锁、可快速切换的怪物外观，并提供「恢复原样」；</li>
 *     <li><b>怪物图鉴</b>：展示全部人形怪物预设，已解锁项可点击变形，未解锁项灰显。</li>
 * </ul>
 * 点击已解锁条目发送 {@link SelectDisguisePacket} 到服务端校验并变形。
 */
public class DisguiseScreen extends Screen {
    private static final int TAB_MEMORY = 0;
    private static final int TAB_BESTIARY = 1;

    private static final int COLS = 6;
    private static final int CELL_W = 56;
    private static final int CELL_H = 74;
    private static final int GRID_TOP = 56;

    private int tab = TAB_MEMORY;
    private int gridLeft;

    public DisguiseScreen() {
        super(Component.translatable("gui.guimi_mod.disguise.title"));
    }

    @Override
    protected void init() {
        int gridW = COLS * CELL_W;
        this.gridLeft = (this.width - gridW) / 2;
    }

    /** 当前标签页要展示的条目 ID 列表。 */
    private List<ResourceLocation> entries(Player player) {
        List<ResourceLocation> list = new ArrayList<>();
        if (tab == TAB_BESTIARY) {
            for (HumanoidDisguises.Entry e : HumanoidDisguises.all()) {
                list.add(e.id());
            }
        } else {
            Set<ResourceLocation> unlocked = player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES).resolve();
            list.addAll(unlocked);
        }
        return list;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return;
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFD700);

        // 标签页按钮
        drawTab(graphics, mouseX, mouseY, 0, Component.translatable("gui.guimi_mod.disguise.tab.memory"), tab == TAB_MEMORY);
        drawTab(graphics, mouseX, mouseY, 1, Component.translatable("gui.guimi_mod.disguise.tab.bestiary"), tab == TAB_BESTIARY);

        DisguiseUnlocks unlocks = player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES);
        DisguiseData current = player.getData(ModAttachments.DISGUISE_STATE);
        List<ResourceLocation> list = entries(player);

        // 记忆库空态提示
        if (tab == TAB_MEMORY && list.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.guimi_mod.disguise.empty"),
                    this.width / 2, GRID_TOP + 30, 0xAAAAAA);
        }

        ResourceLocation hovered = null;
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = list.get(i);
            int cx = gridLeft + (i % COLS) * CELL_W;
            int cy = GRID_TOP + (i / COLS) * CELL_H;
            boolean unlocked = unlocks.contains(id);
            boolean active = current.isMob() && id.equals(current.mobId());
            boolean inBox = isInBox(mouseX, mouseY, cx, cy, CELL_W, CELL_H);

            // 单元格背景
            int bg = active ? 0x8055FF55 : (inBox ? 0x66FFFFFF : 0x44000000);
            graphics.fill(cx + 2, cy + 2, cx + CELL_W - 2, cy + CELL_H - 2, bg);

            HumanoidDisguises.Entry entry = HumanoidDisguises.get(id);
            if (entry != null && (unlocked || tab == TAB_BESTIARY)) {
                renderPreview(graphics, entry, cx, cy, unlocked);
            }

            // 名称
            Component name = entry != null ? entry.type().getDescription()
                    : Component.translatable("gui.guimi_mod.disguise.unknown");
            String s = this.font.plainSubstrByWidth(name.getString(), CELL_W - 6);
            graphics.drawString(this.font, s, cx + (CELL_W - this.font.width(s)) / 2, cy + CELL_H - 12,
                    unlocked ? 0xFFFFFF : 0x808080, false);

            if (inBox) {
                hovered = id;
            }
        }

        // 悬停提示
        if (hovered != null) {
            HumanoidDisguises.Entry entry = HumanoidDisguises.get(hovered);
            boolean unlocked = unlocks.contains(hovered);
            List<Component> tip = new ArrayList<>();
            tip.add(entry != null ? entry.type().getDescription() : Component.literal(hovered.toString()));
            tip.add(Component.translatable(unlocked
                    ? "gui.guimi_mod.disguise.click_morph"
                    : "gui.guimi_mod.disguise.locked_hint").withStyle(
                    unlocked ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.GRAY));
            graphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }

        // 恢复原样按钮
        Component revert = Component.translatable("gui.guimi_mod.disguise.revert");
        int rw = this.font.width(revert) + 12;
        int rx = this.width / 2 - rw / 2;
        int ry = this.height - 30;
        boolean revertHover = isInBox(mouseX, mouseY, rx, ry, rw, 16);
        graphics.fill(rx, ry, rx + rw, ry + 16, revertHover ? 0x88FF5555 : 0x66883333);
        graphics.drawString(this.font, revert, rx + 6, ry + 4, 0xFFFFFF);
    }

    private void drawTab(GuiGraphics graphics, int mouseX, int mouseY, int index, Component label, boolean selected) {
        int tw = 90;
        int tx = this.width / 2 - tw + index * tw;
        int ty = 32;
        boolean hover = isInBox(mouseX, mouseY, tx, ty, tw, 18);
        int color = selected ? 0x99FFD700 : (hover ? 0x66FFFFFF : 0x44000000);
        graphics.fill(tx, ty, tx + tw, ty + 18, color);
        graphics.drawCenteredString(this.font, label, tx + tw / 2, ty + 5, selected ? 0x000000 : 0xFFFFFF);
    }

    /** 在单元格内渲染怪物实时预览；未解锁时叠加暗色蒙版与问号。 */
    private void renderPreview(GuiGraphics graphics, HumanoidDisguises.Entry entry, int cx, int cy, boolean unlocked) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        LivingEntity mob = DisguiseRenderHandler.getOrCreate(entry.type(), this.minecraft.level);
        if (mob == null) {
            return;
        }
        int x1 = cx + 8;
        int y1 = cy + 6;
        int x2 = cx + CELL_W - 8;
        int y2 = cy + CELL_H - 16;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, x1, y1, x2, y2, 24, 0.0625F,
                (x1 + x2) / 2.0F, y1, mob);
        if (!unlocked) {
            graphics.fill(cx + 2, cy + 2, cx + CELL_W - 2, cy + CELL_H - 14, 0xCC101010);
            graphics.drawCenteredString(this.font, "?", cx + CELL_W / 2, cy + CELL_H / 2 - 8, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // 标签页切换
        for (int i = 0; i < 2; i++) {
            int tw = 90;
            int tx = this.width / 2 - tw + i * tw;
            if (isInBox((int) mouseX, (int) mouseY, tx, 32, tw, 18)) {
                this.tab = i;
                return true;
            }
        }
        // 恢复原样
        Component revert = Component.translatable("gui.guimi_mod.disguise.revert");
        int rw = this.font.width(revert) + 12;
        int rx = this.width / 2 - rw / 2;
        int ry = this.height - 30;
        if (isInBox((int) mouseX, (int) mouseY, rx, ry, rw, 16)) {
            PacketDistributor.sendToServer(new SelectDisguisePacket(""));
            return true;
        }
        // 条目选择
        DisguiseUnlocks unlocks = player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES);
        List<ResourceLocation> list = entries(player);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = list.get(i);
            int cx = gridLeft + (i % COLS) * CELL_W;
            int cy = GRID_TOP + (i / COLS) * CELL_H;
            if (isInBox((int) mouseX, (int) mouseY, cx, cy, CELL_W, CELL_H) && unlocks.contains(id)) {
                PacketDistributor.sendToServer(new SelectDisguisePacket(id.toString()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean isInBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
