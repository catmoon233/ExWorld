package com.wan.gmmod.content.entities;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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
 * 噩梦邪眼（隐者途径「噩梦邪眼的角膜 / 脓液」配方材料的来源生物）。
 * <p>
 * <b>双形态实体</b>：单个实体类 + 单个渲染器，按战斗状态在两套 GeckoLib
 * 模型 / 纹理 / 动画之间动态切换（见 {@link com.wan.gmmod.client.render.NightmareEyeModel}）：
 * <ul>
 *   <li><b>常态</b>：无目标时悬浮游荡，使用「常态模型」（fly 悬浮）；</li>
 *   <li><b>攻击形态</b>：锁定目标后张开利爪，切换为「攻击模型」，近身撕咬（attck）。</li>
 * </ul>
 * 形态状态通过 {@link SynchedEntityData} 服务端权威写入并同步到客户端，
 * 渲染器据此在每帧选择对应资源，实现无缝变形。
 */
public class NightmareEyeEntity extends Monster implements GeoEntity {
    /** 是否处于攻击形态（同步到客户端驱动渲染器切换模型）。 */
    private static final EntityDataAccessor<Boolean> ATTACK_FORM =
            SynchedEntityData.defineId(NightmareEyeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attck");

    private static final String CONTROLLER = "controller";
    private static final String TRIGGER_ATTACK = "attck";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NightmareEyeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        // 常驻空中：不受重力，始终悬浮飞行
        this.setNoGravity(true);
        this.xpReward = 12;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACK_FORM, false);
    }

    /** 是否处于攻击形态（客户端渲染器据此切换模型 / 纹理 / 动画文件）。 */
    public boolean isAttackForm() {
        return this.entityData.get(ATTACK_FORM);
    }

    /** 服务端切换形态。 */
    public void setAttackForm(boolean attackForm) {
        this.entityData.set(ATTACK_FORM, attackForm);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal(this, 0.3));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            // 有目标 → 攻击形态；无目标 → 收拢回常态
            boolean shouldAttack = this.getTarget() != null;
            if (isAttackForm() != shouldAttack) {
                setAttackForm(shouldAttack);
            }
            // 悬停保障：意外贴地时缓慢抬升回空中
            if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.06, 0.0));
            }
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
        // 常态与攻击形态共用 fly 悬浮动画名；attck 由近战命中触发（按当前形态的动画文件解析）
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, state ->
                state.setAndContinue(FLY))
                .triggerableAnim(TRIGGER_ATTACK, ATTACK_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
