package com.wan.gmmod.common.item;

import com.wan.gmmod.client.render.DawnSwordRenderer;
import net.minecraft.world.item.Item;
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
 * 晨曦之剑。
 * <p>
 * 使用 GeckoLib 渲染 3D 模型（黎明骑士途径的标志性武器），
 * 继承 {@link SwordItem}，可手持造成伤害。
 */
public class DawnSwordItem extends SwordItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DawnSwordItem(Properties properties) {
        super(Tiers.DIAMOND, properties
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4F))
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
            private DawnSwordRenderer renderer;

            @Override
            public GeoItemRenderer<DawnSwordItem> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new DawnSwordRenderer();
                return this.renderer;
            }
        });
    }
}