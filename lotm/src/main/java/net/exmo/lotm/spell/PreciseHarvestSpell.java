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

/** Next dig drops the block itself, with a chance of one extra. 15 mana, 30s cooldown. */
public final class PreciseHarvestSpell extends InstantSpell {
    public static final int DURATION_TICKS = 20 * 60 * 10;
    public static final float EXTRA_CHANCE = 0.35F;

    public PreciseHarvestSpell() {
        super("precise_harvest", SchoolRegistry.NATURE_RESOURCE, 30.0, 15, SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        caster.addEffect(new MobEffectInstance(LotmEffects.PRECISE_HARVEST, DURATION_TICKS, 0, false, true, true));
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("spell.lotm.precise_harvest.ready"), true);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.precise_harvest.guide"));
    }
}
