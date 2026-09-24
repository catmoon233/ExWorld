package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.ThiefPassives;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/** Leave a stationary afterimage. A hit pops it and slows the attacker. */
public final class ThoughtMisleadSpell extends InstantSpell {
    public ThoughtMisleadSpell() {
        super("thought_mislead", 30.0, 20, SchoolRegistry.ELDRITCH_RESOURCE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ILLUSIONER_MIRROR_MOVE);
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        ThiefPassives.leaveAfterimage(level, caster);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.thought_mislead.guide"));
    }
}
