package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker. The client highlights nearby ores, plants, and creatures while this lasts. */
public final class ResourceSenseEffect extends MobEffect {
    public ResourceSenseEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC6A15B);
    }
}
