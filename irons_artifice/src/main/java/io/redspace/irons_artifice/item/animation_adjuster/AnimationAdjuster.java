package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.client.gun.GunRenderContext;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public interface AnimationAdjuster {
    AnimationAdjuster LOWER_HAMMER = new LowerHammerAdjuster();
    AnimationAdjuster DOUBLE_BARREL_HAMMER = new DoubleBarrelHammerAdjuster();
    AnimationAdjuster HARMONICA_MAGAZINE = new HarmonicaMagazineAdjuster();
    AnimationAdjuster MUZZLE_LOAD_OFFSET = new MuzzleLoadOffsetAdjuster();

    void adjust(BakedGeoModel model, GunRenderContext context);
}
