package com.wan.gmmod.common.event;

import com.wan.gmmod.Config;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

/**
 * 灵界钓鱼：使用 {@link ModItems#SPIRIT_ROD 灵性钓竿} 在夜晚垂钓时，
 * 有概率从守恒池抽出低序列非凡特性（替代本次钓获）。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class AcquisitionFishingSubscriber {

    @SubscribeEvent
    public static void onFishCaught(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (Config.CHAR_FISHING_CHANCE.get() <= 0) {
            return;
        }
        if (!holdsSpiritRod(player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        FishingHook hook = event.getHookEntity();
        // 特殊水域：夜晚的灵性活跃水域
        if (!level.isNight() || hook == null || !hook.isInWater()) {
            return;
        }
        if (level.random.nextDouble() >= Config.CHAR_FISHING_CHANCE.get()) {
            return;
        }
        ItemStack characteristic = CharacteristicManager.drawFromPending(level, level.random);
        if (characteristic == null || characteristic.isEmpty()) {
            return;
        }
        event.getDrops().clear();
        event.getDrops().add(characteristic);
        player.displayClientMessage(Component.translatable("message.guimi_mod.fishing.characteristic"), true);
    }

    private static boolean holdsSpiritRod(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.SPIRIT_ROD.get())
                || player.getOffhandItem().is(ModItems.SPIRIT_ROD.get());
    }
}