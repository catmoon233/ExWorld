package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 魔女教会 - 布朗丝·索伦（魔女途径 · 序列 6：欢愉魔女）。
 * <p>
 * 远程施法者：交替释放「冰晶长枪」与「操控黑焰」魔女技能，贴身时用
 * 黑焰挣脱。生命 60，移速 0.28，追踪距离 24。
 * <p>
 * 动画：{@code animation.idle} 待机 / {@code animation.walk} 移动 /
 * {@code animation.attack} 施法。
 */
public class BrownSilkSolenEntity extends Monster implements GeoEntity, RangedAttackMob {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.walk");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.攻击");

    private static final String CONTROLLER = "controller";
    private static final String TRIGGER_CAST = "cast";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int castCooldown;

    public BrownSilkSolenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 50, 16.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level().isClientSide) return;
        // 交替释放魔女技能：冰晶长枪 / 操控黑焰
        this.castCooldown--;
        boolean ice = (this.castCooldown % 3 == 0) || (this.random.nextInt(100) < 40);
        if (ice) {
            IceSpearEntity spear = new IceSpearEntity(this.level(), this);
            spear.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
            spear.shootFromRotation(this, this.getXRot(), this.getYRot(), 0.0F, 1.8F, 0.5F);
            this.level().addFreshEntity(spear);
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.0F, 1.4F);
        } else {
            // 三朵黑焰霰射
            for (int i = 0; i < 3; i++) {
                BlackFlameEntity flame = new BlackFlameEntity(this.level(), this, 6.0F);
                flame.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
                flame.shootFromRotation(this, this.getXRot(), this.getYRot(), 0.0F, 1.4F, 8.0F);
                this.level().addFreshEntity(flame);
            }
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 0.6F);
        }
        this.triggerAnim(CONTROLLER, TRIGGER_CAST);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK);
            }
            return state.setAndContinue(IDLE);
        }).triggerableAnim(TRIGGER_CAST, CAST));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}