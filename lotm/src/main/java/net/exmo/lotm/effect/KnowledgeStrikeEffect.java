package net.exmo.lotm.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker consumed by the next melee hit: guaranteed crit and partial armor ignore. */
public final class KnowledgeStrikeEffect extends MobEffect {
    public KnowledgeStrikeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2C14E);
    }
}
