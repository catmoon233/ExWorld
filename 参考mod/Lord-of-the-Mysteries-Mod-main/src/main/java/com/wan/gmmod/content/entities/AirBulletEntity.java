package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 空气弹（魔术师 / 无面人「空气弹」能力的弹射物）：无形的压缩空气弹丸。
 * <p>
 * 渲染物品为空气 → 完全不可见，仅命中时爆出一小团云雾粒子。
 * 伤害由发射方传入：魔术师约 5 点，无面人升级为 8 点。
 */
public class AirBulletEntity extends ThrowableItemProjectile {
    /** 本发空气弹的伤害（服务端字段，由发射方设置） */
    private float damage = 5.0F;

    public AirBulletEntity(EntityType<? extends AirBulletEntity> type, Level level) {
        super(type, level);
    }

    public AirBulletEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.AIR_BULLET.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    /** 无形弹射物不受重力影响过大，保持平直弹道 */
    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), this.damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.3,
                        this.random.nextDouble() * 0.2,
                        (this.random.nextDouble() - 0.5) * 0.3);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
