package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.CocoonClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 蛛丝蚕茧的第一人称屏幕层（仅客户端）。
 * <p>
 * 当本地玩家正处于蚕茧包裹状态时，在屏幕最上层叠一层半透明蛛网滤色，营造「身在茧中」的视角。
 * 末尾随剩余时长渐弱；被火烧云时会转为红色滤网并快速消退。观察他人时此层不生效。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class CocoonOverlay {

    /** 第一人称叠加的蛛网纹理 */
    private static final ResourceLocation WEB = GuimiMod.id("textures/gui/web_overlay.png");
    /** 蚕茧初始剩余时长（与 CocoonAbility.DURATION 一致，用于归一化） */
    private static final float BASE_TICKS = 100.0F;

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GuimiMod.id("cocoon_overlay"), CocoonOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        CocoonClientState.Entry entry = CocoonClientState.get(player.getId());
        if (entry == null) {
            return;
        }
        float ratio = Mth.clamp(entry.remainingTicks() / BASE_TICKS, 0.0F, 1.0F);
        // 首段 15% 淡入、末段 25% 淡出
        float enter = Mth.clamp(ratio / 0.15F, 0.0F, 1.0F);
        float exit = Mth.clamp((1.0F - ratio) / 0.25F, 0.0F, 1.0F);
        float strength = Math.min(enter, exit);

        float alpha;
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (entry.burning()) {
            // 火破：红色滤网 + 快速脉动
            float pulse = Mth.sin((player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false)) * 0.4F) * 0.5F + 0.5F;
            alpha = (0.30F + 0.20F * pulse) * strength;
            g = 0.45F + 0.15F * pulse;
            b = 0.30F + 0.10F * pulse;
        } else {
            // 正常：浅白蛛丝网
            alpha = 0.26F * strength;
        }
        if (alpha <= 0.004F) {
            return;
        }

        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        // 强制以含水滤为离屏缓冲后绘制的叠加层，随后的描边仍能正确叠加
        graphics.setColor(r, g, b, alpha);
        graphics.blit(WEB, 0, 0, 0, 0, w, h, w, h);
        graphics.flush();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}