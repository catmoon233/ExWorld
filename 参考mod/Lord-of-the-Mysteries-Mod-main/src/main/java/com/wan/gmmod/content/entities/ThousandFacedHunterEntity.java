package com.wan.gmmod.content.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 千面狩猎者（五头恶龙）：可飞行、会喷吐龙息的巨型飞龙。
 * <ul>
 *   <li>喷吐远程小火球（会吐龙息），近身撕咬</li>
 *   <li>飞行移动，无需重力引导，可空中盘旋</li>
 *   <li>体型：4.0 × 4.0（宽 × 高）；生命 120，攻击 12</li>
 * </ul>
 * 动画：{@code animation.idle} / {@code animation.fly}（飞行）/ {@code animation.walk} / {@code animation.attack}
 */
public class ThousandFacedHunterEntity extends Monster implements GeoEntity, RangedAttackMob {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.attack");

    private static final String CONTROLLER = "controller";
    private static final String TRIGGER_ATTACK = "attack";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ThousandFacedHunterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add( Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 30.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getY(0.5);
        double dz = target.getZ() - this.getZ();
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > 40) return;
        double base = 0.14;
        double vx = dx / d * base;
        double vy = dy / d * base;
        double vz = dz / d * base;
        for (int i = 0; i < 3; i++) {
            SmallFireball fireball = new SmallFireball(this.level(), this,
                    new Vec3(vx + (this.random.nextGaussian() * 0.01),
                            Math.max(vy - 0.02, 0.03) + (this.random.nextGaussian() * 0.005),
                            vz + (this.random.nextGaussian() * 0.01)));
            fireball.setPos(fireball.getX(), this.getY(0.5) + 0.5 + i * 0.2, fireball.getZ());
            this.level().addFreshEntity(fireball);
        }
        if (!this.level().isClientSide) {
            triggerAnim(CONTROLLER, TRIGGER_ATTACK);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !this.level().isClientSide) {
            triggerAnim(CONTROLLER, TRIGGER_ATTACK);
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 5, state -> {
            if (!this.onGround()) {
                return state.setAndContinue(FLY);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }).triggerableAnim(TRIGGER_ATTACK, ATTACK));
    }

    @Override
    public float getScale() {
        return 3.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.isEffectiveAi() && this.onGround() && this.getTarget() != null) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.4, 0));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}