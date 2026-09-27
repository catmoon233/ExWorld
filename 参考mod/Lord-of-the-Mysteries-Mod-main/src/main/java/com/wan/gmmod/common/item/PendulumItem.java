package com.wan.gmmod.common.item;

import com.wan.gmmod.client.render.PendulumRenderer;
import com.wan.gmmod.common.network.packet.PendulumUsePacket;
import com.wan.gmmod.content.divination.DivinationType;
import com.wan.gmmod.content.divination.PendulumDivination;
import com.wan.gmmod.content.divination.PendulumSpin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * 黄水晶灵摆物品。
 * <p>
 * 使用 GeckoLib 渲染 3D 模型（模型 / 动画由用户自行提供）。
 * <ul>
 *     <li><b>右键使用</b>：根据交互方式分为三种占卜——{@link #use} 右键空气（内省）、
 *     {@link #interactLivingEntity} 右键实体（探测）、{@link #useOn} 右键方块（地理），
 *     均在服务端调用 {@link PendulumDivination#perform}。</li>
 *     <li><b>物品动画</b>：客户端收到 {@link PendulumUsePacket} 后调用 {@link #triggerUseAnimation(PendulumSpin)}
 *     播放灵摆摆动动画，动画时长由 {@link PendulumUsePacket#DURATION} 控制。</li>
 *     <li><b>手臂抬起</b>：由 {@code PlayerModelMixin} 结合
 *     {@code PendulumClientState} 直接旋转玩家模型右臂。</li>
 * </ul>
 * 在游戏中的<b>显示位置可手动调节</b>：编辑
 * {@code assets/guimi_mod/models/item/pendulum.json} 的 {@code display} 变换即可。
 */
public class PendulumItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AnimationController<PendulumItem> controller;

    /** 客户端动画状态：是否正在播放使用动画 */
    private boolean playingUse = false;
    /** 客户端动画剩余 tick */
    private int useTicks = 0;
    /** 客户端当前摆动方向，决定播放哪段动画 */
    private PendulumSpin currentResult = PendulumSpin.STILL;

    /** 注册后的单例引用，供网络包在客户端触发动画 */
    private static PendulumItem instance;

    public PendulumItem(Properties properties) {
        super(properties);
        instance = this;
    }

    /**
     * 客户端收到 {@link PendulumUsePacket} 后触发灵摆使用动画。
     *
     * @param result 摆动方向，决定顺时针 / 逆时针 / 静止动画
     */
    public static void triggerUseAnimation(PendulumSpin result) {
        if (instance != null) {
            instance.currentResult = result;
            instance.playingUse = true;
            instance.useTicks = PendulumUsePacket.DURATION;
            if (instance.controller != null) {
                instance.controller.forceAnimationReset();
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controller = new AnimationController<>(this, "controller", 0, state -> {
            if (playingUse) {
                // 按占卜结果播放顺时针 / 逆时针 / 静止动画
                return state.setAndContinue(RawAnimation.begin().thenPlay(currentResult.getAnimationName()));
            }
            // 默认待机
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.pendulum.idle"));
        });
        controllers.add(controller);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // 客户端递减动画计时，到期回到待机
        if (level.isClientSide() && playingUse) {
            if (--useTicks <= 0) {
                playingUse = false;
                if (controller != null) {
                    controller.forceAnimationReset();
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 右键空气 / 地面 → 内省占卜（针对自身）
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PendulumDivination.perform(serverPlayer, DivinationType.SELF, null, null);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // 右键生物 / 玩家 → 探测占卜
        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PendulumDivination.perform(serverPlayer, DivinationType.ENTITY, target, null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 右键方块 → 地理占卜
        Level level = context.getLevel();
        if (!level.isClientSide() && context.getPlayer() instanceof ServerPlayer serverPlayer) {
            PendulumDivination.perform(serverPlayer, DivinationType.POSITION, null, context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PendulumRenderer renderer;

            @Override
            public GeoItemRenderer<PendulumItem> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new PendulumRenderer();
                return this.renderer;
            }
        });
    }
}
