package com.wan.gmmod.content.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 怨灵实体（灵体，Bedrock/GeckoLib 动画实体）。
 * <p>
 * 主动攻击玩家的近战怪物，属于 {@link SpiritBeing 灵体}——平时不可见，
 * 只有玩家开启灵视后才能看见（由 {@code WraithRenderer.shouldRender} 控制）。
 * <p>
 * 使用 GeckoLib 播放动画：默认循环待机动画 {@code animation.wraith.idle}，
 * 移动时循环前进动画 {@code animation.wraith.walk}，
 * 命中目标时通过 {@link #triggerAnim} 触发攻击动画 {@code animation.wraith.attack}。
 * 模型、纹理、动画 JSON 由用户自行提供（见 {@code WraithModel}）。
 * <p>
 * 怨灵会飘浮在空中（无重力 + 飞行寻路），像灵体般在空中游荡并追击玩家。
 */
public class WraithEntity extends Monster implements GeoEntity, SpiritBeing {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wraith.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.wraith.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.wraith.attack");

    /** 主控制器名称，triggerAnim 时使用 */
    private static final String CONTROLLER = "controller";
    /** 攻击动画触发名 */
    private static final String TRIGGER_ATTACK = "attack";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public WraithEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // 飘浮在空中：使用飞行移动控制并取消重力
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
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
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !this.level().isClientSide) {
            // 服务端触发攻击动画，GeckoLib 会同步到客户端播放
            triggerAnim(CONTROLLER, TRIGGER_ATTACK);
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 5, state -> {
                    // 移动时播放前进动画，静止时播放待机动画
                    if (state.isMoving()) {
                        return state.setAndContinue(WALK);
                    }
                    return state.setAndContinue(IDLE);
                })
                .triggerableAnim(TRIGGER_ATTACK, ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
