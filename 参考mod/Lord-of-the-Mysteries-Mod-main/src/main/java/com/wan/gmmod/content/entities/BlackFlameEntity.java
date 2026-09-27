package com.wan.gmmod.content.entities;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 黑焰（女巫「操控黑焰」的弹射物）：一团黑色火焰。
 * <p>
 * 仅对「有灵性的目标」有效：玩家（灵性 &gt; 0 或已就职）与本模组的非凡生物。
 * 对普通生物与方块无效——不点燃、不破坏地形，命中处只留下黑色粒子。
 * 伤害由发射方传入：女巫 6 点，欢愉魔女升级为 8 点。
 */
public class BlackFlameEntity extends ThrowableItemProjectile {
    /** 本朵黑焰的伤害（服务端字段，由发射方设置） */
    private float damage = 6.0F;

    public BlackFlameEntity(EntityType<? extends BlackFlameEntity> type, Level level) {
        super(type, level);
    }

    public BlackFlameEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.BLACK_FLAME.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        // 以「黑焰」图标作为外观物品，投掷物即一团可见的黑色火焰
        return ModItems.BLACK_FLAME_ITEM.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    /** 目标是否具有灵性：玩家（灵性 > 0 或已就职）或本模组的非凡生物。 */
    public static boolean isSpiritualTarget(Entity entity) {
        if (entity instanceof Player player) {
            return SpiritualityManager.get(player) > 0
                    || player.getData(ModAttachments.SEQUENCE_LEVEL) > 0;
        }
        if (entity instanceof SpiritBeing) {
            return true;
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                .getNamespace().equals(GuimiMod.MODID);
    }

    @Override
    public void tick() {
        super.tick();
        // 飞行轨迹：黑烟 + 灵魂火粒子拖尾
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity living) {
            living.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage);
            // 黑焰灼烧：黑色火焰附着目标身上持续燃烧
            living.addEffect(new MobEffectInstance(ModEffects.BLACK_FLAME_BURN, 120, 0));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // 不点燃方块，仅爆出黑色火焰粒子
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 14; i++) {
                this.level().addParticle(
                        this.random.nextBoolean() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.4,
                        this.random.nextDouble() * 0.25,
                        (this.random.nextDouble() - 0.5) * 0.4);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
