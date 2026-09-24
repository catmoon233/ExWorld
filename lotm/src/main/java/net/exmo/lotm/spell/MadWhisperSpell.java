package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Madness whisper. 30 mana, 20s cooldown. Weakness and scrambled movement for 5 seconds. */
public final class MadWhisperSpell extends InstantSpell {
    public static final double RANGE = 16.0;

    public MadWhisperSpell() {
        super("mad_whisper", SchoolRegistry.ELDRITCH_RESOURCE, 20.0, 30, SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return LotmSupport.lookedAt(entity, RANGE) != null;
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (!(level instanceof ServerLevel server)) return;
        LivingEntity target = LotmSupport.lookedAt(caster, RANGE);
        if (target != null) LotmSpellRuntime.whisper(server, target);
    }
}
