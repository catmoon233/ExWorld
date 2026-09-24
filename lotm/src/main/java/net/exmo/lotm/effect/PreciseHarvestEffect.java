package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker. The next block break is treated as silk touch, then this is removed. */
public final class PreciseHarvestEffect extends MobEffect {
    public PreciseHarvestEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7EC8E3);
    }
}
