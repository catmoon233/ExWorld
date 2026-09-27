package com.wan.gmmod.common.event;

import com.wan.gmmod.content.ancient.AncientArtifactItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 古代神秘物品的被动效果。
 * <p>
 * 焦灼的圣袍边角（{@code scorched_robe_fragment}）：手持近战命中时附加少量火焰伤害
 * 并点燃目标；5% 概率反噬——引燃持有者自己。
 */
@EventBusSubscriber(modid = com.wan.gmmod.GuimiMod.MODID)
public final class AncientArtifactEventSubscriber {

    /** 手持圣袍边角时近战附加的火焰伤害。 */
    private static final float BONUS_FIRE_DAMAGE = 2.0F;

    /** 反噬引燃自己的概率。 */
    private static final float SELF_IGNITE_CHANCE = 0.05F;

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!AncientArtifactItem.is(held, "scorched_robe_fragment")) {
            return;
        }
        // 附加火焰伤害并点燃目标
        event.setNewDamage(event.getNewDamage() + BONUS_FIRE_DAMAGE);
        event.getEntity().igniteForSeconds(3);
        // 反噬：5% 概率引燃自己
        if (player.getRandom().nextFloat() < SELF_IGNITE_CHANCE) {
            player.igniteForSeconds(2);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("message.guimi_mod.robe_backfire"), true);
        }
    }

    private AncientArtifactEventSubscriber() {
    }
}
