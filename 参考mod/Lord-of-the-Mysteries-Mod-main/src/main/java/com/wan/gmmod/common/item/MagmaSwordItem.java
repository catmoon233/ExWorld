package com.wan.gmmod.common.item;

import com.wan.gmmod.client.render.MagmaSwordRenderer;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * 岩浆之剑。
 * <p>
 * 深渊途径 · 序列6"恶魔"的标志性武器，使用 GeckoLib 渲染 3D 模型，
 * 继承 {@link SwordItem}，可手持造成伤害并点燃目标。
 */
public class MagmaSwordItem extends SwordItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MagmaSwordItem(Properties properties) {
        super(Tiers.NETHERITE, properties
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 4, -2.4F))
                .stacksTo(1));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 静态模型，无需动画控制器
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private MagmaSwordRenderer renderer;

            @Override
            public GeoItemRenderer<MagmaSwordItem> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new MagmaSwordRenderer();
                return this.renderer;
            }
        });
    }
}