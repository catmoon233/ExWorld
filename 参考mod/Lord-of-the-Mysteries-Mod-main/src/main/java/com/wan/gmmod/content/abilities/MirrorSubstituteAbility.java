package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.effects.FallCorruptionEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 「镜子替身」——女巫（魔女途径 · 序列 7）主动 / 被动混合。
 * <p>
 * 需提前潜行右键镜子绑定替身锚点（消耗灵性，见 {@code MirrorItem}）。
 * 手动触发（任意距离）或受到致命伤害时自动触发（见
 * {@code WitchAbilityEventSubscriber#onFatalDamage}）：本体传送回镜子位置，
 * 镜子碎裂（锚点清空）。冷却 120 秒。
 * <p>
 * 欢愉魔女「镜子魔法强化」（{@code mirror_mastery}）解锁后，
 * 触发时若锚点附近 16 格内有其他玩家绑定的镜子锚点亦可作为重生点（此处简化为
 * 传送后不清空锚点的概率保留），占卜成功率提升逻辑见镜子占卜。
 */
public class MirrorSubstituteAbility extends Ability {
    public MirrorSubstituteAbility() {
        super(GuimiMod.id("mirror_substitute"), 20, 120 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        teleportToAnchor(player, false);
    }

    /**
     * 传送到镜子锚点。返回是否成功。
     *
     * @param auto 是否为致命伤自动触发（自动触发时提示文案不同）
     */
    public static boolean teleportToAnchor(Player player, boolean auto) {
        if (!(player instanceof ServerPlayer sp)) {
            return false;
        }
        // 深渊化克制：堕落状态下无法用替身脱身
        if (FallCorruptionEffect.isCorrupted(sp)) {
            sp.displayClientMessage(
                    Component.translatable("message.guimi_mod.fall_corruption.no_substitute"), true);
            return false;
        }
        String anchor = sp.getData(ModAttachments.MIRROR_ANCHOR);
        if (anchor.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror_substitute.no_anchor"), true);
            return false;
        }
        String[] parts = anchor.split(";");
        if (parts.length != 4) {
            sp.setData(ModAttachments.MIRROR_ANCHOR, "");
            return false;
        }
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.parse(parts[0]));
        ServerLevel targetLevel = sp.server.getLevel(dimension);
        if (targetLevel == null) {
            return false;
        }
        BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        // 出发点碎镜粒子
        if (sp.level() instanceof ServerLevel from) {
            from.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                    40, 0.4, 0.8, 0.4, 0.1);
        }
        sp.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                sp.getYRot(), sp.getXRot());
        // 镜子魔法强化：有小概率镜子不碎（锚点保留）
        boolean mastery = SkillManager.isUnlocked(sp, GuimiMod.id("mirror_mastery"));
        if (!mastery || sp.getRandom().nextFloat() >= 0.5F) {
            sp.setData(ModAttachments.MIRROR_ANCHOR, "");
        }
        targetLevel.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                40, 0.4, 0.8, 0.4, 0.1);
        targetLevel.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        sp.displayClientMessage(Component.translatable(auto
                ? "message.guimi_mod.mirror_substitute.auto"
                : "message.guimi_mod.mirror_substitute.done"), true);
        return true;
    }
}
