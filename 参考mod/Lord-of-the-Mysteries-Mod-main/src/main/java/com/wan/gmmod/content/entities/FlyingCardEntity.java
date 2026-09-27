package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 飞牌（小丑「飞牌」能力的弹射物）：锋利的纸牌，支持两种发射模式。
 * <ul>
 *   <li>精准单点：伤害 3 点，命中头部造成双倍伤害；弹道笔直、射程远，白色细尾迹；</li>
 *   <li>散射：单张伤害 2 点，附加「切割流血」（每秒 0.5 点，持续 3 秒），
 *       寿命更短（射程约缩短 20%），淡紫色尾迹。</li>
 * </ul>
 * 由 {@link com.wan.gmmod.content.abilities.FlyingCardAbility} 发射，
 * 使用 {@link com.wan.gmmod.client.render.FlyingCardRenderer} 平放渲染。
 */
public class FlyingCardEntity extends ThrowableItemProjectile {
    /** 是否为散射模式发射（同步给客户端以区分尾迹/渲染） */
    private static final EntityDataAccessor<Boolean> SCATTER =
            SynchedEntityData.defineId(FlyingCardEntity.class, EntityDataSerializers.BOOLEAN);

    /** 精准单点伤害 */
    private static final float PRECISE_DAMAGE = 3.0F;
    /** 散射单张伤害 */
    private static final float SCATTER_DAMAGE = 2.0F;
    /** 精准模式最大寿命（刻）——限制射程 */
    private static final int PRECISE_LIFE = 100;
    /** 散射模式最大寿命（刻）——射程约缩短 20%+ */
    private static final int SCATTER_LIFE = 30;

    private int life;

    public FlyingCardEntity(EntityType<? extends FlyingCardEntity> type, Level level) {
        super(type, level);
    }

    public FlyingCardEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FLYING_CARD.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SCATTER, false);
    }

    public void setScatter(boolean scatter) {
        this.entityData.set(SCATTER, scatter);
    }

    public boolean isScatter() {
        return this.entityData.get(SCATTER);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PAPER_CARD.get();
    }

    @Override
    protected double getDefaultGravity() {
        // 精准模式弹道更笔直
        return this.isScatter() ? 0.03 : 0.012;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // 尾迹粒子：精准 = 白色细尾迹（END_ROD）；散射 = 淡紫色尾迹（DRAGON_BREATH）
            if (this.isScatter()) {
                this.level().addParticle(ParticleTypes.DRAGON_BREATH,
                        this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else {
                this.level().addParticle(ParticleTypes.END_ROD,
                        this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        } else if (++this.life > (this.isScatter() ? SCATTER_LIFE : PRECISE_LIFE)) {
            // 寿命耗尽：纸牌失去锐势坠落消散
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        if (this.isScatter()) {
            // 散射：伤害略低，附加「切割流血」（每秒 0.5 点，持续 3 秒）
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), SCATTER_DAMAGE);
            if (result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 60, 0), this.getOwner());
            }
        } else {
            // 精准：命中头部（眼部附近）造成双倍伤害
            float damage = PRECISE_DAMAGE;
            if (result.getEntity() instanceof LivingEntity living
                    && result.getLocation().y >= living.getEyeY() - 0.25) {
                damage *= 2.0F;
            }
            result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // 命中音效差异：精准 = 尖锐纸割声；散射 = 连续划破声
            if (this.isScatter()) {
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.4F);
            } else {
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 2.0F);
            }
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 6; i++) {
                this.level().addParticle(ParticleTypes.CRIT,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.2,
                        this.random.nextDouble() * 0.2,
                        (this.random.nextDouble() - 0.5) * 0.2);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
