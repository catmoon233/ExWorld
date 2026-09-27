package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 子弹（丧钟发射的弹射物）：按装填的子弹物品呈现外观并触发对应效果。
 * <p>
 * 基础伤害 8 点，低重力保持平直弹道。子弹类型差异：
 * <ul>
 *   <li>剥夺子弹：剥夺目标一项增益效果。</li>
 *   <li>寄生子弹：施加中毒 + 缓慢（寄生侵蚀）。</li>
 *   <li>控灵子弹：命中灵体（游魂 / 怨灵）额外 +6 伤害。</li>
 *   <li>欺瞒子弹：目标短暂迷惑（反胃）并清空仇恨。</li>
 *   <li>驱邪子弹：对不死生物与灵体额外 +6 伤害。</li>
 *   <li>净化子弹：清除目标全部负面效果。</li>
 * </ul>
 */
public class BulletEntity extends ThrowableItemProjectile {
    /** 基础伤害 */
    private static final float BASE_DAMAGE = 8.0F;
    /** 控灵 / 驱邪子弹对特攻目标的额外伤害 */
    private static final float BONUS_DAMAGE = 6.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity shooter, ItemStack bulletStack) {
        super(ModEntities.BULLET.get(), shooter, level);
        // 记录子弹物品：既决定渲染外观，也决定命中效果
        this.setItem(bulletStack.copyWithCount(1));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BULLET.get();
    }

    /** 枪械弹道：低重力保持平直 */
    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;

        Item bullet = this.getItem().getItem();
        Entity hit = result.getEntity();
        float damage = BASE_DAMAGE;

        if (hit instanceof LivingEntity living) {
            // 控灵 / 驱邪：对灵体（不死生物）特攻
            boolean spiritual = hit instanceof SpiritEntity || hit instanceof WraithEntity;
            if (bullet == ModItems.SPIRIT_CONTROL_BULLET.get() && spiritual) {
                damage += BONUS_DAMAGE;
            }
            if (bullet == ModItems.EXORCISM_BULLET.get()
                    && (spiritual || living.isInvertedHealAndHarm())) {
                damage += BONUS_DAMAGE;
            }
        }

        hit.hurt(this.damageSources().thrown(this, this.getOwner()), damage);

        if (hit instanceof LivingEntity living) {
            applyBulletEffect(bullet, living);
        }
    }

    /** 按子弹类型施加命中附加效果。 */
    private void applyBulletEffect(Item bullet, LivingEntity target) {
        if (bullet == ModItems.DEPRIVATION_BULLET.get()) {
            // 剥夺：移除目标一项增益效果
            target.getActiveEffects().stream()
                    .filter(e -> e.getEffect().value().isBeneficial())
                    .findFirst()
                    .ifPresent(e -> target.removeEffect(e.getEffect()));
        } else if (bullet == ModItems.PARASITIC_BULLET.get()) {
            // 寄生：中毒 + 缓慢，模拟寄生侵蚀
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
        } else if (bullet == ModItems.DECEPTION_BULLET.get()) {
            // 欺瞒：短暂迷惑并清空仇恨
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
        } else if (bullet == ModItems.PURIFICATION_BULLET.get()) {
            // 净化：清除目标全部负面效果
            target.getActiveEffects().stream()
                    .filter(e -> !e.getEffect().value().isBeneficial())
                    .map(MobEffectInstance::getEffect)
                    .toList()
                    .forEach(target::removeEffect);
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
