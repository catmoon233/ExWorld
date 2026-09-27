package com.wan.gmmod.content.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 灵体实体（{@link SpiritBeing}，仅灵视可见）。
 * <p>
 * 支持「盟友模式」（魔镜通灵「灵体召唤」）：绑定召唤者后跟随其行动、
 * 协助攻击召唤者的敌人（最近伤害来源 / 攻击目标），到期后化作灵魂粒子消散。
 * 盟友模式下不会以召唤者为目标。
 */
public class SpiritEntity extends Monster implements SpiritBeing {
    /** 盟友跟随距离：超过则寻路靠近，过远则直接闪现到召唤者身边 */
    private static final double FOLLOW_DIST = 4.0;
    private static final double TELEPORT_DIST = 24.0;

    /** 盟友召唤者 UUID，null 表示普通灵体 */
    @Nullable
    private UUID allyOwner;
    /** 盟友存在截止游戏刻，0 表示不限时 */
    private long allyExpire;

    public SpiritEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** 绑定为召唤者的盟友灵体，到期后自动消散。 */
    public void setAlly(Player owner, long expireGameTime) {
        this.allyOwner = owner.getUUID();
        this.allyExpire = expireGameTime;
        this.setPersistenceRequired();
    }

    public boolean isAlly() {
        return allyOwner != null;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // 盟友模式下永不以召唤者为目标
        if (allyOwner != null && target != null && allyOwner.equals(target.getUUID())) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || allyOwner == null) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        // 到期 / 召唤者离线：化作灵魂粒子消散
        Player owner = level.getPlayerByUUID(allyOwner);
        if (owner == null || !owner.isAlive()
                || (allyExpire > 0 && level.getGameTime() >= allyExpire)) {
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    getX(), getY() + 1.0, getZ(), 16, 0.3, 0.6, 0.3, 0.02);
            discard();
            return;
        }
        // 每半秒维护一次跟随与协战目标
        if (tickCount % 10 != 0) {
            return;
        }
        // 协战：优先攻击伤害召唤者的敌人，其次召唤者正在攻击的目标
        LivingEntity enemy = owner.getLastHurtByMob();
        if (enemy == null || !enemy.isAlive()) {
            enemy = owner.getLastHurtMob();
        }
        if (enemy != null && enemy.isAlive() && enemy != this
                && !allyOwner.equals(enemy.getUUID())) {
            setTarget(enemy);
        }
        // 跟随：无战斗目标时靠近召唤者，过远直接闪现
        double dist = distanceTo(owner);
        if (dist > TELEPORT_DIST) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
        } else if (getTarget() == null && dist > FOLLOW_DIST) {
            getNavigation().moveTo(owner, 1.2);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (allyOwner != null) {
            tag.putUUID("AllyOwner", allyOwner);
            tag.putLong("AllyExpire", allyExpire);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("AllyOwner")) {
            allyOwner = tag.getUUID("AllyOwner");
            allyExpire = tag.getLong("AllyExpire");
        }
    }
}
