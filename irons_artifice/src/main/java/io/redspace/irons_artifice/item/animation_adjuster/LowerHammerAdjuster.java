package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.client.gun.GunRenderContext;
import io.redspace.irons_artifice.item.MagazineContents;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public final class LowerHammerAdjuster implements AnimationAdjuster {
    @Override
    public void adjust(BakedGeoModel model, GunRenderContext context) {
        MagazineContents magazineContents = context.magazine;
        GeoBone bone = model.getBone(GunBones.HAMMER).orElse(null);
        if (bone == null || magazineContents == null) {
            return;
        }
        if (magazineContents.isEmpty() && context.reloadProgressSeconds <= 0) {
            bone.updateRotation(0, 0, 0);
        }
    }
}
