package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class StealthSpell extends InstantLotmSpell {
    public StealthSpell() {
        super("stealth", SchoolRegistry.ELDRITCH_RESOURCE, 20, 20);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (caster instanceof ServerPlayer player) LotmRuntime.stealth(player);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.stealth.guide"));
    }
}
