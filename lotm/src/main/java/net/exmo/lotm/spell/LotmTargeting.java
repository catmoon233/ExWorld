package net.exmo.lotm.spell;

import net.exmo.lotm.LotmSupport;
import net.minecraft.world.entity.LivingEntity;

/** Crosshair lookup kept for spells that already call it. */
public final class LotmTargeting {
    private LotmTargeting() {}

    public static LivingEntity living(LivingEntity caster, double range) {
        return LotmSupport.lookedAt(caster, range);
    }
}
