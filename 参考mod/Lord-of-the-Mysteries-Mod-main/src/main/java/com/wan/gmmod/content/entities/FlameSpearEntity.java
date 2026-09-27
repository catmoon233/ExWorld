package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 炽白之枪——纵火家（战争之红途径 · 序列 7）「炽白之枪」投掷的火焰长枪。
 * <p>
 * 伤害 9，直线穿透（命中生物后不消失），命中方块时引导附近火球向命中点靠拢。
 */
public class FlameSpearEntity extends ThrowableItemProjectile {

    public FlameSpearEntity(EntityType<? extends FlameSpearEntity> type, Level level) {
        super(type, level);
    }

    public FlameSpearEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FLAME_SPEAR.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLAME_SPEAR_ITEM.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        } else if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 穿透：伤害后不销毁，继续直线飞行
        if (!this.level().isClientSide && result.getEntity() != this.getOwner()) {
            result.getEntity().hurt(this.damageSources().indirectMagic(this, this.getOwner()), 9.0F);
            result.getEntity().igniteForSeconds(4);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            // 引导附近（16 米内）的火球向命中点靠拢
            Vec3 hit = result.getLocation();
            List<FlameOrbEntity> orbs = this.level().getEntitiesOfClass(FlameOrbEntity.class,
                    this.getBoundingBox().inflate(16.0));
            for (FlameOrbEntity orb : orbs) {
                Vec3 toHit = hit.subtract(orb.position()).normalize().scale(0.8);
                orb.setDeltaMovement(toHit);
            }
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 16; i++) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.5,
                        this.random.nextDouble() * 0.3,
                        (this.random.nextDouble() - 0.5) * 0.5);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
