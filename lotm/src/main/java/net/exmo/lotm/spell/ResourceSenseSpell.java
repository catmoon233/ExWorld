package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/** Highlight nearby ores, plants, and creatures for 20 seconds. 10 mana, 25s cooldown. */
public final class ResourceSenseSpell extends InstantSpell {
    public static final int DURATION_TICKS = 20 * 20;

    public ResourceSenseSpell() {
        super("resource_sense", SchoolRegistry.NATURE_RESOURCE, 25.0, 10, SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(LotmEffects.RESOURCE_SENSE, DURATION_TICKS, 0, false, true, true));
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("spell.lotm.resource_sense.ready"), true);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.resource_sense.guide"));
    }
}
