package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.client.gun.GunRenderContext;
import io.redspace.irons_artifice.item.MagazineContents;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public final class DoubleBarrelHammerAdjuster implements AnimationAdjuster {
    @Override
    public void adjust(BakedGeoModel model, GunRenderContext context) {
        MagazineContents magazineContents = context.magazine;
        GeoBone left = model.getBone(GunBones.HAMMER_LEFT).orElse(null);
        GeoBone right = model.getBone(GunBones.HAMMER_RIGHT).orElse(null);
        if (left == null || right == null || magazineContents == null) {
            return;
        }
        if (context.reloadProgressSeconds <= 1.17) {
            if (magazineContents.count() <= 1) {
                left.updateRotation(0, 0, 0);
            }
            if (magazineContents.isEmpty()) {
                right.updateRotation(0, 0, 0);
            }
        }
    }
}
