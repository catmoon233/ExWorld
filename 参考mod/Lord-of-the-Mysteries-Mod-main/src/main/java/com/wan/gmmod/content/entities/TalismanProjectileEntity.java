package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.talisman.TalismanData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * 灵性符咒投射物：念咒灌注后由玩家投掷，飞行约 0.5 秒，
 * 在落点 / 命中目标处激发对应祈求对象的效果。
 * <ul>
 *   <li>净化符咒（祈求太阳）：对鬼魂类怪物造成重创，对不死生物造成伤害；</li>
 *   <li>安魂符咒（祈求黑夜）：安抚鬼魂、幽影、僵尸与水鬼（减速 + 虚弱 + 取消仇恨），
 *       对强力怨魂造成一定伤害；</li>
 *   <li>电击符咒（祈求暴君）：引发闪电与电击，范围内造成雷电伤害。</li>
 * </ul>
 */
public class TalismanProjectileEntity extends ThrowableItemProjectile {

    /** 效果半径 */
    private static final double RADIUS = 4.0;
    /** 鬼魂类重创伤害（净化） */
    private static final float GHOST_DAMAGE = 8.0F;
    /** 不死生物伤害（净化） */
    private static final float UNDEAD_DAMAGE = 4.0F;
    /** 怨魂 / 恶灵伤害（安魂） */
    private static final float EVIL_SPIRIT_DAMAGE = 5.0F;
    /** 电击范围伤害 */
    private static final float ELECTRIC_DAMAGE = 6.0F;

    public TalismanProjectileEntity(EntityType<? extends TalismanProjectileEntity> type, Level level) {
        super(type, level);
    }

    public TalismanProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.TALISMAN.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BLANK_TALISMAN.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        trigger((ServerLevel) this.level(), result.getEntity().getX(),
                result.getEntity().getY(), result.getEntity().getZ());
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            trigger((ServerLevel) this.level(), this.getX(), this.getY(), this.getZ());
        }
    }

    /** 在命中点激发符咒效果。 */
    private void trigger(ServerLevel level, double x, double y, double z) {
        TalismanData data = this.getItem().get(ModDataComponents.TALISMAN.get());
        String type = data != null ? data.type() : "";
        switch (type) {
            case "purification" -> triggerPurification(level, x, y, z);
            case "requiem" -> triggerRequiem(level, x, y, z);
            case "electric" -> triggerElectric(level, x, y, z);
            default -> { }
        }
        // 激发音效 + 客户端粒子
        level.playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        level.broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    /** 净化：重创鬼魂类，伤害不死生物。 */
    private void triggerPurification(ServerLevel level, double x, double y, double z) {
        for (LivingEntity target : entitiesInRadius(level, x, y, z)) {
            if (isGhostLike(target)) {
                target.hurt(this.damageSources().magic(), GHOST_DAMAGE);
            } else if (target.getType().is(EntityTypeTags.UNDEAD)) {
                target.hurt(this.damageSources().magic(), UNDEAD_DAMAGE);
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 40, 1.5, 1.5, 1.5, 0.05);
    }

    /** 安魂：安抚鬼魂 / 幽影 / 僵尸 / 水鬼，对强力怨魂造成伤害。 */
    private void triggerRequiem(ServerLevel level, double x, double y, double z) {
        for (LivingEntity target : entitiesInRadius(level, x, y, z)) {
            if (!isGhostLike(target) && !target.getType().is(EntityTypeTags.UNDEAD)) {
                continue;
            }
            // 安抚：减速 + 虚弱 + 取消仇恨
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), this.getOwner());
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), this.getOwner());
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
            // 强力怨魂 / 恶灵：一定程度造成伤害
            if (isGhostLike(target)) {
                target.hurt(this.damageSources().magic(), EVIL_SPIRIT_DAMAGE);
            }
        }
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 40, 1.5, 1.5, 1.5, 0.05);
    }

    /** 电击：引发 2~3 道随机闪电并造成范围雷电伤害。 */
    private void triggerElectric(ServerLevel level, double x, double y, double z) {
        int count = 2 + this.random.nextInt(2); // 2~3 道
        for (int i = 0; i < count; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                double ox = (this.random.nextDouble() - 0.5) * 4.0;
                double oz = (this.random.nextDouble() - 0.5) * 4.0;
                bolt.moveTo(x + ox, y, z + oz);
                level.addFreshEntity(bolt);
            }
        }
        for (LivingEntity target : entitiesInRadius(level, x, y, z)) {
            target.hurt(this.damageSources().lightningBolt(), ELECTRIC_DAMAGE);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 60, 1.5, 1.5, 1.5, 0.1);
    }

    private List<LivingEntity> entitiesInRadius(ServerLevel level, double x, double y, double z) {
        AABB box = new AABB(x - RADIUS, y - RADIUS, z - RADIUS,
                x + RADIUS, y + RADIUS, z + RADIUS);
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this.getOwner() && e.isAlive());
    }

    /** 鬼魂类：模组怨灵 / 灵体 / 阴影生物。 */
    private static boolean isGhostLike(LivingEntity entity) {
        return entity instanceof WraithEntity
                || entity instanceof SpiritEntity
                || entity instanceof ShadowCreatureEntity;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 12; i++) {
                this.level().addParticle(ParticleTypes.SPIT,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.4,
                        this.random.nextDouble() * 0.4,
                        (this.random.nextDouble() - 0.5) * 0.4);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}