package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 火鸦——纵火家（战争之红途径 · 序列 7）「火鸦术」召唤的追踪火焰。
 * <p>
 * 每只伤害 2 并点燃目标，飞行时自动追踪 15 米内的锁定目标；
 * 目标死亡或超出范围则直线飞行，10 秒后自毁。
 */
public class FireRavenEntity extends ThrowableItemProjectile {
    /** 追踪目标的 UUID（服务端字段）。 */
    private UUID targetId;
    private int life;

    public FireRavenEntity(EntityType<? extends FireRavenEntity> type, Level level) {
        super(type, level);
    }

    public FireRavenEntity(Level level, LivingEntity shooter, LivingEntity target) {
        super(ModEntities.FIRE_RAVEN.get(), shooter, level);
        if (target != null) {
            this.targetId = target.getUUID();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FIRE_RAVEN_ITEM.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            return;
        }
        if (++this.life > 200) {
            this.discard();
            return;
        }
        // 追踪：向 15 米内的锁定目标转向
        if (this.targetId != null && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && serverLevel.getEntity(this.targetId) instanceof LivingEntity target
                && target.isAlive() && this.distanceTo(target) <= 15.0F) {
            Vec3 toTarget = target.getEyePosition().subtract(this.position()).normalize().scale(0.6);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6).add(toTarget.scale(0.4)).normalize().scale(0.6));
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() != this.getOwner()) {
            result.getEntity().hurt(this.damageSources().indirectMagic(this, this.getOwner()), 2.0F);
            result.getEntity().igniteForSeconds(3);
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
                this.level().addParticle(ParticleTypes.FLAME,
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
