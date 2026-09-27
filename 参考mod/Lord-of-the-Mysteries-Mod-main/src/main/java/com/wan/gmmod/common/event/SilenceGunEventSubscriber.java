package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.SilenceGunItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 寂灭左键开枪事件：
 * <ul>
 *   <li>左键攻击实体：取消近战攻击，改为开枪。</li>
 *   <li>左键点击方块：取消挖掘，改为开枪。</li>
 *   <li>左键空挥：客户端事件，见 GMmodClient 中发送 SilenceGunFirePacket。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class SilenceGunEventSubscriber {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof SilenceGunItem gun)) {
            return;
        }
        // 持枪时左键不近战，改为开枪
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            gun.fire(player);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity().getMainHandItem().getItem() instanceof SilenceGunItem gun)) {
            return;
        }
        // 持枪时左键不挖掘方块，改为开枪
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            gun.fire(player);
        }
    }
}
