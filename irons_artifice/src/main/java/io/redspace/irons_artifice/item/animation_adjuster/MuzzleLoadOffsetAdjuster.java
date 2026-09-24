package io.redspace.irons_artifice.item.animation_adjuster;

import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.client.gun.GunRenderContext;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public final class MuzzleLoadOffsetAdjuster implements AnimationAdjuster {
    private static final float IN_END = 0.20f;
    private static final float OUT_START = 0.80f;

    @Override
    public void adjust(BakedGeoModel model, GunRenderContext context) {
        float muzzleOffset = context.muzzleOffset;
        float reloadPercent = context.reloadPercent;
        if (muzzleOffset == 0f || reloadPercent == 0f) {
            return;
        }
        float weight = envelope(reloadPercent);
        if (weight == 0f) {
            return;
        }
        float offset = muzzleOffset * 16 * weight;
        model.getBone(GunBones.GUN).ifPresent(bone -> bone.setPosZ(bone.getPosZ() + offset));
        model.getBone(GunBones.RAMROD).ifPresent(bone -> bone.setPosZ(bone.getPosZ() - offset));
    }

    private static float envelope(float t) {
        if (t < IN_END) {
            return easeInOutSine(t / IN_END);
        }
        if (t > OUT_START) {
            return 1f - easeInOutSine((t - OUT_START) / (1f - OUT_START));
        }
        return 1f;
    }

    private static float easeInOutSine(float x) {
        return 0.5f - 0.5f * Mth.cos(Mth.PI * x);
    }
}
