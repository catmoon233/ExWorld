package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.content.effects.FallCorruptionEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 「魔杖替身」——女巫（魔女途径 · 序列 7）主动 / 被动混合。
 * <p>
 * 需提前潜行右键魔杖绑定（写入 {@code ModDataComponents#WAND_BOND}，见
 * {@code WandItem}）。魔杖可交给其他玩家携带；手动触发或受到致命伤害时
 * （镜子锚点缺失时的后备，见 {@code WitchAbilityEventSubscriber}），
 * 本体传送到持有绑定魔杖的玩家身边，实现远程救援。冷却 120 秒。
 */
public class WandSubstituteAbility extends Ability {
    public WandSubstituteAbility() {
        super(GuimiMod.id("wand_substitute"), 20, 120 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        teleportToWand(player, false);
    }

    /** 传送到全服范围内持有本人绑定魔杖的玩家身边。返回是否成功。 */
    public static boolean teleportToWand(Player player, boolean auto) {
        if (!(player instanceof ServerPlayer sp)) {
            return false;
        }
        // 深渊化克制：堕落状态下无法用替身脱身
        if (FallCorruptionEffect.isCorrupted(sp)) {
            sp.displayClientMessage(
                    Component.translatable("message.guimi_mod.fall_corruption.no_substitute"), true);
            return false;
        }
        String selfUuid = sp.getUUID().toString();
        for (ServerPlayer holder : sp.server.getPlayerList().getPlayers()) {
            if (!hasBoundWand(holder, selfUuid)) {
                continue;
            }
            if (sp.level() instanceof ServerLevel from) {
                from.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                        40, 0.4, 0.8, 0.4, 0.1);
            }
            ServerLevel targetLevel = holder.serverLevel();
            sp.teleportTo(targetLevel, holder.getX(), holder.getY(), holder.getZ(),
                    sp.getYRot(), sp.getXRot());
            targetLevel.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                    40, 0.4, 0.8, 0.4, 0.1);
            targetLevel.playSound(null, holder.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
            sp.displayClientMessage(Component.translatable(auto
                    ? "message.guimi_mod.wand_substitute.auto"
                    : "message.guimi_mod.wand_substitute.done", holder.getDisplayName()), true);
            return true;
        }
        sp.displayClientMessage(Component.translatable("message.guimi_mod.wand_substitute.no_wand"), true);
        return false;
    }

    /** 该玩家背包中是否有绑定指定女巫的魔杖。 */
    private static boolean hasBoundWand(ServerPlayer holder, String witchUuid) {
        for (ItemStack stack : holder.getInventory().items) {
            if (witchUuid.equals(stack.get(ModDataComponents.WAND_BOND.get()))) {
                return true;
            }
        }
        return witchUuid.equals(holder.getOffhandItem().get(ModDataComponents.WAND_BOND.get()));
    }
}
