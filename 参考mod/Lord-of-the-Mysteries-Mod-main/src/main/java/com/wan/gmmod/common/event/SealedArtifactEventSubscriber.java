package com.wan.gmmod.common.event;

import com.wan.gmmod.Config;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.SealedArtifactItem;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.content.characteristics.SealedArtifactData;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 封印物事件监听：持有 / 穿戴期间的持续效果。
 * <ul>
 *     <li><b>正面效果</b>：背包 / 盔甲 / 副手持有封印物时，持续获得途径对应的增益，
 *         强度随封印特性的序列等级缩放（序列 0 最强）。</li>
 *     <li><b>负面代价</b>：同时承受「封印侵蚀」，力量越大侵蚀越猛；卸下封印物后自然消散。</li>
 * </ul>
 * 不灭保护（不消失 / 不坠入虚空）由 {@link CharacteristicEventSubscriber} 统一处理。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class SealedArtifactEventSubscriber {

    /** 效果刷新窗口（刻）：每 tick 续满，卸下后约 2 秒内消散。 */
    private static final int EFFECT_TICKS = 40;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.isDeadOrDying()) {
            return;
        }
        SealedArtifactData data = findSealedArtifact(player);
        if (data == null) {
            return;
        }
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        if (pathway == null) {
            return;
        }

        int posAmp = SealedArtifactItem.positiveAmplifier(data.level());
        if (posAmp >= 1) {
            player.addEffect(new MobEffectInstance(SealedArtifactItem.positiveEffect(pathway),
                    EFFECT_TICKS, posAmp, false, false, true));
        }
        int negAmp = SealedArtifactItem.negativeAmplifier(data.level());
        if (negAmp > 0 && isDemigod(player)) {
            // 非凡物品：半神及以上非凡者持有封印物时，封印侵蚀减弱
            negAmp = Math.max(0, negAmp - Config.SEALED_ARTIFACT_DEMIGOD_REDUCTION.get());
        }
        if (negAmp >= 1) {
            player.addEffect(new MobEffectInstance(ModEffects.SEALED_CORRUPTION,
                    EFFECT_TICKS, negAmp, false, false, true));
        }
    }

    /** 是否为半神及以上层次非凡者（已就职且序列号 ≤ 阈值，序列号越小越强）。 */
    private static boolean isDemigod(Player player) {
        int level = player.getData(ModAttachments.SEQUENCE_LEVEL);
        return level > 0 && level <= Config.SEALED_ARTIFACT_DEMIGOD_THRESHOLD.get();
    }

    /** 扫描玩家全部背包（物品栏 + 盔甲 + 副手），返回找到的第一件封印物数据。 */
    private static SealedArtifactData findSealedArtifact(Player player) {
        for (var slot : player.getInventory().items) {
            SealedArtifactData data = SealedArtifactItem.getData(slot);
            if (data != null) {
                return data;
            }
        }
        for (var slot : player.getInventory().armor) {
            SealedArtifactData data = SealedArtifactItem.getData(slot);
            if (data != null) {
                return data;
            }
        }
        for (var slot : player.getInventory().offhand) {
            SealedArtifactData data = SealedArtifactItem.getData(slot);
            if (data != null) {
                return data;
            }
        }
        return null;
    }
}
