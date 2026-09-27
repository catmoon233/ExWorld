package com.wan.gmmod.content.abilities;

import com.wan.gmmod.Config;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 天使级「特性放牧」——序列 2 及以上的被动聚合能力（聚合定律）。
 * <p>
 * 周期性扫描极远距离内的同 / 相近途径特性物品，缓缓将其吸引向玩家，
 * 体现「承载者相互靠近」的命运调整。搜索半径取配置 {@link Config#CHARACTERISTIC_ATTRACTION_RANGE} 的数倍。
 * 注册到各途径序列 0 ~ 2，由 {@link com.wan.gmmod.common.event.GameEventSubscriber} 的被动 tick 驱动。
 */
public class CharacteristicHerdingAbility extends Ability {
    /** 天使级放牧半径为普通吸引半径的倍数 */
    private static final double RANGE_MULTIPLIER = 8.0;
    /** 每 20 刻（1 秒）放牧一次，降低性能开销 */
    private static final int INTERVAL = 20;

    public CharacteristicHerdingAbility() {
        super(GuimiMod.id("characteristic_herding"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide() || player.tickCount % INTERVAL != 0) {
            return;
        }
        Sequences.Pathway pathway = Sequences.fromKey(player.getData(ModAttachments.PATHWAY));
        if (pathway == null) {
            return;
        }
        double range = Config.CHARACTERISTIC_ATTRACTION_RANGE.get() * RANGE_MULTIPLIER;
        Vec3 center = player.position();
        List<ItemEntity> items = CharacteristicManager.nearbyCharacteristicItems(player.level(), center, range);
        for (ItemEntity item : items) {
            var data = com.wan.gmmod.common.item.CharacteristicItem.getData(item.getItem());
            if (data == null) {
                continue;
            }
            Sequences.Pathway itemPathway = Sequences.fromKey(data.pathway());
            if (itemPathway == null || !pathway.isProximate(itemPathway)) {
                continue;
            }
            Vec3 dir = new Vec3(
                    player.getX() - item.getX(),
                    player.getY() + 0.5 - item.getY(),
                    player.getZ() - item.getZ()).normalize();
            // 缓缓吸引：叠加一个小速度矢量
            item.setDeltaMovement(item.getDeltaMovement().add(dir.scale(0.08)));
            item.hasImpulse = true;
        }
    }
}
