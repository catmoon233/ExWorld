package io.redspace.irons_artifice.item;

import com.llamalad7.mixinextras.lib.apache.commons.mutable.MutableObject;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;


public class BaseGeoItem extends Item implements GeoItem {
    public final MutableObject<GeoRenderProvider> geoRenderProvider = new MutableObject<>();
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public BaseGeoItem(Item.Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(java.util.function.Consumer<GeoRenderProvider> consumer) {
        // GeckoLib memoizes the first provider. Delegate so client setup can still fill geoRenderProvider.
        consumer.accept(new GeoRenderProvider() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                GeoRenderProvider delegate = BaseGeoItem.this.geoRenderProvider.getValue();
                return delegate == null ? null : delegate.getGeoItemRenderer();
            }
        });
    }

    @Override
    public void registerControllers(final AnimatableManager.@NotNull ControllerRegistrar controllers) {
    }

    @Override
    public @NotNull AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
