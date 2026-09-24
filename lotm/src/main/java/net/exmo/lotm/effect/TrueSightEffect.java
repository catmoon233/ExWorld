package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect. The client uses it to reveal hidden entities. */
public final class TrueSightEffect extends MobEffect {
    public TrueSightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7AD7FF);
    }
}
