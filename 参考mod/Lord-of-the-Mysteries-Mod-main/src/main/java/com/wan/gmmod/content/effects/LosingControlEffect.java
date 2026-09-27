package com.wan.gmmod.content.effects;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 「失控」效果：晋升仪式失败的惩罚（默认持续 5 分钟）。
 * <p>
 * 服务端周期行为（由 AI 接管部分操作）：
 * <ul>
 *     <li>随机瞬移：低概率把玩家瞬移到附近随机位置；</li>
 *     <li>攻击附近生物：强制对最近的生物挥击一次；</li>
 *     <li>疯狂低语：向该玩家单独播放低语音效（个人化，他人听不见）。</li>
 * </ul>
 * 此外：
 * <ul>
 *     <li>期间无法使用序列能力（见 {@code SkillManager.trigger} 的失控校验）；</li>
 *     <li>屏幕边缘扭曲噪点由客户端 {@code LosingControlOverlay} 绘制。</li>
 * </ul>
 */
public class LosingControlEffect extends MobEffect {
    /** 默认持续时间：5 分钟 */
    public static final int DEFAULT_DURATION = 5 * 60 * 20;
    /** 行为判定间隔（刻） */
    private static final int ACTION_INTERVAL = 40;
    /** 随机瞬移半径 */
    private static final double TELEPORT_RADIUS = 8.0;

    public LosingControlEffect() {
        super(MobEffectCategory.HARMFUL, 0x5b2c83);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % ACTION_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return true;
        }
        RandomSource random = player.getRandom();

        // 疯狂低语：单独发给该玩家本人（个人化幻听）
        if (random.nextFloat() < 0.6F) {
            Holder<net.minecraft.sounds.SoundEvent> sound =
                    Holder.direct(com.wan.gmmod.common.registry.ModSounds.MAD_WHISPER.get());
            player.connection.send(new ClientboundSoundPacket(sound, SoundSource.AMBIENT,
                    player.getX(), player.getY(), player.getZ(), 0.9F,
                    0.8F + random.nextFloat() * 0.4F, random.nextLong()));
        }

        // 随机瞬移
        if (random.nextFloat() < 0.35F) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RADIUS;
            double y = player.getY() + (random.nextInt(9) - 4);
            double z = player.getZ() + (random.nextDouble() - 0.5) * 2.0 * TELEPORT_RADIUS;
            player.randomTeleport(x, y, z, true);
        }

        // AI 接管：强制攻击附近最近的生物
        if (random.nextFloat() < 0.5F) {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(3.0),
                    e -> e != player && e.isAlive() && !(e instanceof Player p && !level.getServer().isPvpAllowed() && p != player));
            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(random.nextInt(targets.size()));
                player.attack(target);
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            }
        }
        return true;
    }
}
