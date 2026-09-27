package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * 魔术师「伤害转移」——序列 7 被动能力（无面人效果不变）。
 * <p>
 * 受到致命伤害时不会立即死亡，而是转为 30 点虚假血量；
 * 期间受到的伤害优先扣除虚假血量，虚假血量每秒自然衰减 1 点直到扣完。
 * 致命伤拦截与伤害吸收在 {@link com.wan.gmmod.common.event.AbilityEventSubscriber} 中处理，
 * 这里只负责虚假血量的持续衰减。
 */
public class DamageTransferAbility extends Ability {

    public DamageTransferAbility() {
        super(GuimiMod.id("damage_transfer"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        int fake = player.getData(ModAttachments.FAKE_HEALTH);
        if (fake > 0) {
            player.setData(ModAttachments.FAKE_HEALTH, fake - 1);
        }
    }
}
