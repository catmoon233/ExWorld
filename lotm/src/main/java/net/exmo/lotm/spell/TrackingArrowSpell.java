package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Tracking arrow. 15 mana, 8s cooldown. A hit bleeds for 5 seconds. */
public final class TrackingArrowSpell extends InstantSpell {
    public TrackingArrowSpell() {
        super("tracking_arrow", SchoolRegistry.BLOOD_RESOURCE, 8.0, 15, SoundEvents.ARROW_SHOOT);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (level instanceof ServerLevel server) LotmSpellRuntime.launchArrow(server, caster);
    }
}
