package net.exmo.lotm.sequence;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.compat.Curios;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/**
 * Sequence actives are owned spells, not spellbook entries.
 * Ownership is answered by {@link AbstractSpellLearnedMixin}. Casting uses the selection event,
 * which adds a wheel slot without writing the spell into a deck.
 */
public final class SequenceSpellOwnership {
    private SequenceSpellOwnership() {}

    public static boolean owns(Player player, ResourceLocation spellId) {
        if (player == null || spellId == null) return false;
        if (player.level().isClientSide()) return ClientSequenceBridge.owns(spellId);
        return SequenceService.ownsSpell(player, spellId);
    }

    @SubscribeEvent
    public static void selection(SpellSelectionManager.SpellSelectionEvent event) {
        Player player = event.getEntity();
        List<SequenceSkill> spells = player.level().isClientSide()
                ? ClientSequenceBridge.spells()
                : SequenceService.ownedSpells(player);
        String slot = Curios.SPELLBOOK_SLOT == null ? "spellbook" : Curios.SPELLBOOK_SLOT;
        int index = 0;
        for (SequenceSkill skill : spells) {
            AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(skill.ref().toString()));
            if (spell == null || spell == SpellRegistry.none()) continue;
            event.addSelectionOption(new SpellData(spell, skill.level()), slot, 900 + index);
            index++;
        }
    }
}
