package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
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
 * 成年独角兽（月亮途径配方材料的来源生物，GeckoLib 动画实体）。
 * <p>
 * 中立生物：平时温顺游荡，被攻击后用独角反击。
 * 动画资源：{@code animations/entity/adult_unicorn.animation.json}
 * （animation.idle / run / walk / attack / horn_attack）。
 */
public class AdultUnicornEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.run");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.attack");
    private static final RawAnimation HORN_ATTACK = RawAnimation.begin().thenPlay("animation.horn_attack");

    private static final String CONTROLLER = "controller";
    private static final String TRIGGER_ATTACK = "attack";
    private static final String TRIGGER_HORN = "horn_attack";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AdultUnicornEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // 被激怒后冲撞反击
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.3, true));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !this.level().isClientSide) {
            // 概率使用独角突刺
            if (this.random.nextBoolean()) {
                triggerAnim(CONTROLLER, TRIGGER_HORN);
            } else {
                triggerAnim(CONTROLLER, TRIGGER_ATTACK);
            }
            this.playSound(ModSounds.WOLFMAN_ATTACK.get(), 0.8F, 1.2F);
        }
        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 5, state -> {
            if (state.isMoving()) {
                double speed = this.getDeltaMovement().horizontalDistanceSqr();
                return state.setAndContinue(speed > 0.12 ? RUN : WALK);
            }
            return state.setAndContinue(IDLE);
        }).triggerableAnim(TRIGGER_ATTACK, ATTACK)
          .triggerableAnim(TRIGGER_HORN, HORN_ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
