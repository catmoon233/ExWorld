package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * 变身原模型隐藏（仅客户端）。
 * <p>
 * 在 {@link RenderPlayerEvent.Pre} 中按变身效果隐藏玩家原本的 {@link PlayerModel} 部件，
 * 避免与 {@link TransformVisualLayer} 叠加渲染的狼人化 / 恶魔化模型重叠：
 * <ul>
 *     <li>恶魔化（{@code demon_form}）：全身模型覆盖，隐藏玩家全部部件；</li>
 *     <li>狼人化（{@code werewolf_form}）：叠加模型只有躯干 / 双臂 / 尾巴，
 *         隐藏玩家躯干与双臂（含衣物），头部与双腿保留玩家部件。</li>
 * </ul>
 * 效果消失后显式恢复全部部件可见，避免残留。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class TransformRenderHandler {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        PlayerModel<?> model = playerRenderer.getModel();
        boolean demon = player.hasEffect(ModEffects.DEMON_FORM);
        boolean werewolf = player.hasEffect(ModEffects.WEREWOLF_FORM);
        boolean bat = player.hasEffect(ModEffects.BAT_FORM);
        boolean hideAll = demon || bat;
        model.head.visible = !hideAll;
        model.hat.visible = !hideAll;
        model.body.visible = !hideAll && !werewolf;
        model.jacket.visible = !hideAll && !werewolf;
        model.rightArm.visible = !hideAll && !werewolf;
        model.rightSleeve.visible = !hideAll && !werewolf;
        model.leftArm.visible = !hideAll && !werewolf;
        model.leftSleeve.visible = !hideAll && !werewolf;
        model.rightLeg.visible = !hideAll;
        model.rightPants.visible = !hideAll;
        model.leftLeg.visible = !hideAll;
        model.leftPants.visible = !hideAll;
    }
}
