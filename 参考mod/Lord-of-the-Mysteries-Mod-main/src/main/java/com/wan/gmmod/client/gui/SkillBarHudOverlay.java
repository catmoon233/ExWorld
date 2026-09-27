package com.wan.gmmod.client.gui;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.SkillPageClientState;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.SkillBarData;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 技能栏 HUD：在屏幕左下（物品栏左侧）绘制 15 个（3 页 × 5 槽，每列一页）技能图标。
 * <p>
 * 每格显示：能力图标、冷却遮罩与剩余冷却秒数、灵性消耗数字、以及当前不可用时的灰显。
 * 分页反馈：当前页列边框高亮（紧急页为红色）、切页时短暂闪烁、页内选中槽金色描边、
 * 网格上方渐隐显示当前页码。所有判定基于已同步的附件（技能栏、冷却、灵性、途径、序列等）。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class SkillBarHudOverlay {
    private static final ResourceLocation SLOT_TEX = GuimiMod.id("textures/gui/skill_slot.png");
    private static final ResourceLocation SLOT_DISABLED_TEX = GuimiMod.id("textures/gui/skill_slot_disabled.png");
    private static final ResourceLocation COOLDOWN_TEX = GuimiMod.id("textures/gui/cooldown_mask.png");
    /** 飞牌模式小图标：精准单点 / 散射（8×8，绘制在飞牌槽位右下角） */
    private static final ResourceLocation CARD_MODE_PRECISE_TEX = GuimiMod.id("textures/gui/skills/card_mode_precise.png");
    private static final ResourceLocation CARD_MODE_SCATTER_TEX = GuimiMod.id("textures/gui/skills/card_mode_scatter.png");

    private static final int COLS = 3;
    private static final int ROWS = 5;
    private static final int SLOT = 20;
    private static final int ICON = 16;
    private static final int MARGIN = 4;

    /** 当前页列边框颜色（灵青）/ 紧急页红色 / 切页闪烁亮白 / 页内选中槽金色 */
    private static final int PAGE_BORDER = 0xFF55CCFF;
    private static final int PAGE_BORDER_EMERGENCY = 0xFFFF4444;
    private static final int PAGE_BORDER_FLASH = 0xFFFFFFFF;
    private static final int SELECTED_BORDER = 0xFFFFD54F;

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        // Skill bar is retired. Casting goes through Iron's spell wheel.
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.getData(ModAttachments.PATHWAY).isEmpty()) {
            return;
        }
        if (mc.options.hideGui || com.wan.gmmod.client.HudClientState.isHidden()) {
            return;
        }

        SkillBarData bar = player.getData(ModAttachments.SKILL_BAR);
        int spirituality = SpiritualityManager.get(player);

        int gridH = ROWS * SLOT;
        int baseX = MARGIN;
        int baseY = mc.getWindow().getGuiScaledHeight() - gridH - MARGIN;

        int page = SkillPageClientState.getPage();
        int selectedGlobal = SkillPageClientState.getSelectedGlobalSlot();

        for (int i = 0; i < SkillBarData.SIZE; i++) {
            // 列主序：每列 = 一页（列 = i/5，行 = i%5），与技能页划分一致
            int sx = baseX + (i / ROWS) * SLOT;
            int sy = baseY + (i % ROWS) * SLOT;

            ResourceLocation id = bar.get(i);
            Ability ability = id == null ? null : AbilityRegistry.getById(id);

            boolean unlocked = ability != null && SkillManager.isUnlocked(player, id);
            boolean enoughSpirit = ability == null || spirituality >= ability.getSpiritualityCost();
            boolean greyed = ability != null && (!unlocked || !enoughSpirit);
            long remaining = ability == null ? 0L : SkillManager.cooldownRemaining(player, id);

            // 槽位背景（不可用时用灰显背景）
            graphics.blit(greyed ? SLOT_DISABLED_TEX : SLOT_TEX, sx, sy, 0, 0, SLOT, SLOT, SLOT, SLOT);

            // 当前页列边框高亮：紧急页红色、切页时短暂闪烁；其他页槽位保持正常
            if (i / SkillPageClientState.SLOTS_PER_PAGE == page) {
                int border = SkillPageClientState.isFlashBright() ? PAGE_BORDER_FLASH
                        : (SkillPageClientState.isEmergencyPage() ? PAGE_BORDER_EMERGENCY : PAGE_BORDER);
                drawOutline(graphics, sx, sy, SLOT, SLOT, border);
            }
            // Ctrl+滚轮选中的槽：金色双层描边
            if (i == selectedGlobal) {
                drawOutline(graphics, sx, sy, SLOT, SLOT, SELECTED_BORDER);
                drawOutline(graphics, sx + 1, sy + 1, SLOT - 2, SLOT - 2, SELECTED_BORDER);
            }

            if (ability == null) {
                continue;
            }

            // 能力图标
            graphics.blit(ability.getIconTexture(), sx + 2, sy + 2, 0, 0, ICON, ICON, ICON, ICON);

            // 灰显：不可用（途径/序列不符或灵性不足）时叠加暗色蒙版
            if (greyed) {
                graphics.fill(sx + 2, sy + 2, sx + 2 + ICON, sy + 2 + ICON, 0x99202020);
            }

            // 冷却遮罩 + 剩余秒数
            if (remaining > 0L) {
                graphics.blit(COOLDOWN_TEX, sx, sy, 0, 0, SLOT, SLOT, SLOT, SLOT);
                String secs = String.valueOf((remaining + 19) / 20);
                int tx = sx + (SLOT - mc.font.width(secs)) / 2;
                int ty = sy + (SLOT - mc.font.lineHeight) / 2;
                graphics.drawString(mc.font, secs, tx, ty, 0xFFFFFF);
            }

            // 飞牌槽位：右下角绘制当前发射模式小图标（精准单点 / 散射）
            boolean flyingCard = GuimiMod.id("flying_card").equals(id);
            if (flyingCard) {
                ResourceLocation modeTex = player.getData(ModAttachments.CARD_SCATTER_MODE)
                        ? CARD_MODE_SCATTER_TEX : CARD_MODE_PRECISE_TEX;
                graphics.blit(modeTex, sx + SLOT - 9, sy + SLOT - 9, 0, 0, 8, 8, 8, 8);
            }

            // 灵性消耗数字（不足时显示红色，足够显示青色；飞牌槽位让位给模式图标，移到左下角）
            int cost = ability.getSpiritualityCost();
            if (cost > 0) {
                String costStr = String.valueOf(cost);
                int cx = flyingCard ? sx + 2 : sx + SLOT - mc.font.width(costStr) - 2;
                int cy = sy + SLOT - mc.font.lineHeight;
                graphics.drawString(mc.font, costStr, cx, cy, enoughSpirit ? 0x55CCFF : 0xFF5555);
            }
        }

        // 网格上方渐隐显示当前页码（切页 / 页内选择时短暂可见，紧急页显红色）
        float alpha = SkillPageClientState.getIndicatorAlpha();
        if (alpha > 0.05F) {
            boolean emergency = SkillPageClientState.isEmergencyPage();
            Component label = emergency
                    ? Component.translatable("hud.guimi_mod.skill_page.emergency")
                    : Component.translatable("hud.guimi_mod.skill_page", page + 1);
            int color = ((int) (alpha * 255.0F) << 24) | (emergency ? 0xFF5555 : 0xFFFFFF);
            graphics.drawString(mc.font, label, baseX, baseY - mc.font.lineHeight - 3, color);
        }
    }

    /** 绘制 1 像素矩形描边。 */
    private static void drawOutline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}
