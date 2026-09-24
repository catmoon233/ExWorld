package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.HunterTraps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class PlaceTrapSpell extends InstantLotmSpell {
    public static final float DAMAGE = 6.0F;

    public PlaceTrapSpell() {
        super("place_trap", SchoolRegistry.FIRE_RESOURCE, 20, 15);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.TRIPWIRE_CLICK_ON);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (!(level instanceof ServerLevel server) || !(caster instanceof ServerPlayer player)) return;
        HunterTraps.place(server, player, SpellTargets.trapAnchor(caster, 8.0), DAMAGE, getSpellResource());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.place_trap.guide"));
    }
}
