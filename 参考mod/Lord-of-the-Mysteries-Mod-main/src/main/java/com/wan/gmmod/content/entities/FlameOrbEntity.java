package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 火球——纵火家（战争之红途径 · 序列 7）「火球术」「巨大火球」的弹射物。
 * <p>
 * 三档形态由发射方传入：
 * <ul>
 *   <li>赤红火球：伤害 5，爆炸范围 2 米；</li>
 *   <li>炽白火球：伤害 10，爆炸范围 4 米；</li>
 *   <li>巨大火球：伤害 18，爆炸范围 6 米，冲击波击退。</li>
 * </ul>
 * 爆炸不破坏地形，命中区域内生物受火焰伤害并燃烧。
 */
public class FlameOrbEntity extends ThrowableItemProjectile {
    private float damage = 5.0F;
    private float radius = 2.0F;
    /** 炽白形态（客户端同名粒子判定用实体事件广播，无需同步字段）。 */
    private boolean white = false;
    /** 巨大火球：爆炸附带冲击波击退。 */
    private boolean giant = false;

    public FlameOrbEntity(EntityType<? extends FlameOrbEntity> type, Level level) {
        super(type, level);
    }

    public FlameOrbEntity(Level level, LivingEntity shooter, float damage, float radius,
                          boolean white, boolean giant) {
        super(ModEntities.FLAME_ORB.get(), shooter, level);
        this.damage = damage;
        this.radius = radius;
        this.white = white;
        this.giant = giant;
        // 巨大火球使用专属外观物品渲染（普通/炽白火球走 getDefaultItem）
        if (giant) {
            this.setItem(new ItemStack(ModItems.GIANT_FLAME_ORB_ITEM.get()));
        }
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FLAME_ORB_ITEM.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            explode(this.level(), this.position(),
                    this.getOwner() instanceof LivingEntity living ? living : null,
                    this.damage, this.radius, this.giant);
            this.level().broadcastEntityEvent(this, this.white ? (byte) 4 : (byte) 3);
            this.discard();
        }
    }

    /**
     * 火焰爆炸的通用实现：范围内生物受伤 + 燃烧 4 秒，不破坏地形。
     *
     * @param knockback 是否附带冲击波击退（巨大火球）
     */
    public static void explode(Level level, Vec3 center, LivingEntity owner,
                               float damage, float radius, boolean knockback) {
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                e -> e != owner && e.isAlive());
        for (LivingEntity victim : victims) {
            victim.hurt(owner != null
                    ? level.damageSources().indirectMagic(owner, owner)
                    : level.damageSources().magic(), damage);
            victim.igniteForSeconds(4);
            if (knockback) {
                Vec3 push = victim.position().subtract(center).normalize().scale(1.5).add(0, 0.4, 0);
                victim.push(push.x, push.y, push.z);
                victim.hurtMarked = true;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3 || id == 4) {
            // 3=赤红爆炸，4=炽白爆炸
            for (int i = 0; i < 30; i++) {
                this.level().addParticle(
                        id == 4 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.8,
                        this.random.nextDouble() * 0.5,
                        (this.random.nextDouble() - 0.5) * 0.8);
            }
            this.level().addParticle(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
