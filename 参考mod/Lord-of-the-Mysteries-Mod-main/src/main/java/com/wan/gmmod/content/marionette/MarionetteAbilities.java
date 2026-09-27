package com.wan.gmmod.content.marionette;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 秘偶原有非凡能力映射（共享视野右键触发）。
 * <p>
 * 按秘偶种类映射其标志性能力：
 * <ul>
 *   <li>苦力怕 → 点燃自爆；</li>
 *   <li>末影人 → 沿视线定向瞬移；</li>
 *   <li>烈焰人 → 喷吐小火球；</li>
 *   <li>恶魂 → 发射爆炸火球；</li>
 *   <li>远程型（骷髅 / 女巫 / 溺尸等 {@link RangedAttackMob}）→ 向拾取目标释放远程攻击。</li>
 * </ul>
 * {@link #hasAbility} 双端可用（HUD 灰显判定）；{@link #perform} 仅服务端调用。
 */
public final class MarionetteAbilities {
    /** 末影人定向瞬移的最大距离（米） */
    private static final double BLINK_RANGE = 24.0;

    private MarionetteAbilities() {}

    /** 该秘偶是否保留可释放的原有非凡能力（客户端 HUD 与服务端共用判定）。 */
    public static boolean hasAbility(Mob mob) {
        return mob instanceof Creeper || mob instanceof EnderMan
                || mob instanceof Blaze || mob instanceof Ghast
                || mob instanceof RangedAttackMob;
    }

    /**
     * 触发秘偶的原有非凡能力（仅服务端）。
     *
     * @param target 玩家视线拾取到的目标，可为 null
     * @return false 表示该能力需要目标但未拾取到目标
     */
    public static boolean perform(ServerPlayer owner, Mob mob, LivingEntity target) {
        ServerLevel level = owner.serverLevel();
        if (mob instanceof Creeper creeper) {
            // 苦力怕：点燃自爆（秘偶的谢幕演出，爆炸后操控自动解除）
            creeper.ignite();
            owner.displayClientMessage(
                    Component.translatable("ability.guimi_mod.shared_vision.creeper_ignite"), true);
            return true;
        }
        if (mob instanceof EnderMan enderMan) {
            // 末影人：沿视线定向瞬移（撞到方块则落在其前方）
            Vec3 start = enderMan.getEyePosition();
            Vec3 look = enderMan.getViewVector(1.0F);
            BlockHitResult hit = level.clip(new ClipContext(start, start.add(look.scale(BLINK_RANGE)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, enderMan));
            Vec3 dest = hit.getLocation().subtract(look.scale(0.5));
            if (enderMan.randomTeleport(dest.x, dest.y, dest.z, true)) {
                level.playSound(null, enderMan.blockPosition(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
            } else {
                owner.displayClientMessage(
                        Component.translatable("ability.guimi_mod.shared_vision.teleport_failed"), true);
            }
            return true;
        }
        if (mob instanceof Blaze blaze) {
            // 烈焰人：沿视线喷吐小火球
            Vec3 look = blaze.getViewVector(1.0F);
            SmallFireball fireball = new SmallFireball(level, blaze, look);
            fireball.setPos(blaze.getEyePosition().add(look.scale(0.6)));
            level.addFreshEntity(fireball);
            level.playSound(null, blaze.blockPosition(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }
        if (mob instanceof Ghast ghast) {
            // 恶魂：沿视线发射爆炸火球
            Vec3 look = ghast.getViewVector(1.0F);
            LargeFireball fireball = new LargeFireball(level, ghast, look, ghast.getExplosionPower());
            fireball.setPos(ghast.getEyePosition().add(look.scale(1.0)));
            level.addFreshEntity(fireball);
            level.playSound(null, ghast.blockPosition(),
                    SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }
        if (mob instanceof RangedAttackMob ranged) {
            // 远程型：需要拾取目标
            if (target == null) {
                return false;
            }
            ranged.performRangedAttack(target, 1.0F);
            return true;
        }
        // 调用前应已用 hasAbility 过滤，走不到这里
        return false;
    }
}
