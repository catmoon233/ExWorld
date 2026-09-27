package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.MagicArtifactItem;
import com.wan.gmmod.content.magic.MagicArtifactManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 神奇物品事件监听。
 * <ul>
 *   <li><b>戏法大师·变体2</b>：折叠空间存储满 24 小时未取出的物品被自动弹出。</li>
 *   <li><b>记录官·变体3</b>：记录的能力超过 24 小时未使用则遭受反噬。</li>
 *   <li><b>右键触发</b>：携带 {@code MAGIC_ARTIFACT} 组件的物品（含「类似附魔」后
 *       保留的基底物品）右键时激活对应能力。</li>
 *   <li><b>tooltip</b>：为这类物品附加能力说明。</li>
 * </ul>
 * 能力触发逻辑见 {@link MagicArtifactManager}。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class MagicArtifactEventSubscriber {

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel)) {
            return;
        }
        if (player.isDeadOrDying()) {
            return;
        }
        ServerPlayer sp = (ServerPlayer) player;
        MagicArtifactManager.tickFoldAutoEject(sp);
        MagicArtifactManager.tickRecordRebound(sp);
    }

    /** 类似附魔：携带 {@code MAGIC_ARTIFACT} 组件的物品右键触发能力（服务端权威）。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (MagicArtifactItem.getData(stack) == null) {
            return;
        }
        if (MagicArtifactManager.onUse(event.getEntity(), stack)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** 神奇物品（含附魔后保留的基底物品）的能力 tooltip。 */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        MagicArtifactItem.appendMagicTooltip(event.getItemStack(), event.getToolTip());
    }
}
