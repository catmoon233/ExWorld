package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 冰晶长枪（欢愉魔女「冰霜强化」的投掷物）。
 * <p>
 * 伤害 8 点，命中后目标冰冻 3 秒（极限缓慢 + 冻结视觉）+ 后续减速 5 秒。
 * 渲染物品为空气，以蓝色冰晶粒子拖尾呈现枪体。
 */
public class IceSpearEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 8.0F;
    /** 冰冻时长（刻，3 秒） */
    private static final int FREEZE_TICKS = 3 * 20;
    /** 后续减速时长（刻，5 秒） */
    private static final int SLOW_TICKS = 5 * 20;

    public IceSpearEntity(EntityType<? extends IceSpearEntity> type, Level level) {
        super(type, level);
    }

    public IceSpearEntity(Level level, LivingEntity shooter) {
        super(ModEntities.ICE_SPEAR.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        // 以「冰晶长枪」图标作为外观物品：投掷时能看到纹理发射出去
        return ModItems.ICE_SPEAR_ITEM.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.02;
    }

    @Override
    public void tick() {
        super.tick();
        // 冰晶拖尾
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        if (result.getEntity() instanceof LivingEntity target) {
            target.hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
            // 冰冻 3 秒（极限缓慢近似定身）+ 冻结视觉，随后减速 5 秒
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, 255, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS + SLOW_TICKS, 1, false, true));
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), FREEZE_TICKS + 140));
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
            for (int i = 0; i < 12; i++) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE,
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
