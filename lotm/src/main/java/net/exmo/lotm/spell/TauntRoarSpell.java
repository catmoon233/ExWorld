package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Taunt roar. 20 mana, 12s cooldown. Does not spend mana if nobody is in range. */
public final class TauntRoarSpell extends InstantSpell {
    public TauntRoarSpell() {
        super("taunt_roar", SchoolRegistry.EVOCATION_RESOURCE, 12.0, 20, SoundEvents.RAVAGER_ROAR);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return LotmSpellRuntime.hasTauntTarget(entity);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (level instanceof ServerLevel server) LotmSpellRuntime.roar(server, caster);
    }
}
