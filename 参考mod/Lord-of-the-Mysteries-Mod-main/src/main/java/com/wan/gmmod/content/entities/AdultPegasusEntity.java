package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 成年飞马（月亮途径「成年独角飞马」配方材料的来源生物，GeckoLib 动画实体）。
 * <p>
 * 中立飞行生物：翱翔于天空，被攻击后俯冲反击。
 * 动画资源：{@code animations/entity/adult_pegasus.animation.json}
 * （animation.idle / run / attack / fly）。
 */
public class AdultPegasusEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.run");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.attack");

    private static final String CONTROLLER = "controller";
    private static final String TRIGGER_ATTACK = "attack";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AdultPegasusEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
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
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 0.35));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2, true));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !this.level().isClientSide) {
            triggerAnim(CONTROLLER, TRIGGER_ATTACK);
            this.playSound(ModSounds.WOLFMAN_ATTACK.get(), 0.8F, 1.1F);
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 5, state -> {
            // 空中展翅飞行；地面奔跑；静止待机
            if (!this.onGround() && !this.isPassenger()) {
                return state.setAndContinue(FLY);
            }
            if (state.isMoving()) {
                return state.setAndContinue(RUN);
            }
            return state.setAndContinue(IDLE);
        }).triggerableAnim(TRIGGER_ATTACK, ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
