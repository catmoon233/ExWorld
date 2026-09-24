package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker. The client outlines nearby ores, traps, and creatures while this lasts. */
public final class SpiritVisionEffect extends MobEffect {
    public SpiritVisionEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB388FF);
    }
}
