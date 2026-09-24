package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.client.gun.GunRenderContext;
import io.redspace.irons_artifice.item.MagazineContents;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public final class HarmonicaMagazineAdjuster implements AnimationAdjuster {
    @Override
    public void adjust(BakedGeoModel model, GunRenderContext context) {
        MagazineContents magazineContents = context.magazine;
        GeoBone magazine = model.getBone(GunBones.MAGAZINE).orElse(null);
        if (magazine == null || magazineContents == null) {
            return;
        }
        if (context.reloadProgressSeconds <= 0.42) {
            float percent = 1 - magazineContents.count() / 10f;
            magazine.updatePosition(4 * percent, 0, 0);
        }
    }
}
