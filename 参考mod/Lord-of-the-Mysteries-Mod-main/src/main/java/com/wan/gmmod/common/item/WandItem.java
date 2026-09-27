package com.wan.gmmod.common.item;

import com.wan.gmmod.client.render.WandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class WandItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private AnimationController<WandItem> controller;
    private AnimationController<WandItem> switchController;

    private String currentAnimation = "idle";
    private boolean isSwitching = false;
    private boolean wasInMainHand = false;
    private int switchCooldown = 0;

    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controller = new AnimationController<>(this, "controller", 0, state -> {
            if (isSwitching) {
                return PlayState.STOP;
            }
            if ("attack".equals(currentAnimation)) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.attack"));
            } else if ("draw".equals(currentAnimation)) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.draw"));
            } else {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.idle"));
            }
        });
        
        switchController = new AnimationController<>(this, "switch_controller", 0, state -> {
            if (isSwitching) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.cane.switch"));
            }
            return PlayState.STOP;
        });
        
        controllers.add(controller);
        controllers.add(switchController);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() && entity instanceof Player player) {
            // 检测主手是否持有本物品
            ItemStack mainHandStack = player.getMainHandItem();
            boolean inMainHand = !mainHandStack.isEmpty() && mainHandStack.getItem() == this;
            
            // 从其他物品切换到手杖
            if (inMainHand && !wasInMainHand && switchCooldown <= 0) {
                isSwitching = true;
                switchCooldown = 30; // 30 ticks (1.5秒) 冷却，匹配切换动画时长
                if (switchController != null) {
                    switchController.forceAnimationReset();
                }
                currentAnimation = "idle";
                if (controller != null) {
                    controller.forceAnimationReset();
                }
            }
            // 冷却倒计时
            if (switchCooldown > 0) {
                switchCooldown--;
                if (switchCooldown <= 0) {
                    isSwitching = false;
                }
            }
            wasInMainHand = inMainHand;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private WandRenderer renderer;

            @Override
            public GeoItemRenderer<WandItem> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new WandRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            currentAnimation = "draw";
            if (controller != null) {
                controller.forceAnimationReset();
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, net.minecraft.world.entity.Entity entity) {
        if (player.level().isClientSide()) {
            currentAnimation = "attack";
            if (controller != null) {
                controller.forceAnimationReset();
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }
}