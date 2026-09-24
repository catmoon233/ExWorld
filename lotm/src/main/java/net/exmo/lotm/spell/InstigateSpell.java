package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class InstigateSpell extends InstantLotmSpell {
    public InstigateSpell() {
        super("instigate", SchoolRegistry.ELDRITCH_RESOURCE, 25, 25);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_PREPARE_ATTACK);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        return SpellTargets.living(caster, 16.0) != null;
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        LivingEntity target = SpellTargets.living(caster, 16.0);
        if (target == null) return;
        LotmRuntime.instigate(caster, target);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.instigate.guide"));
    }
}
