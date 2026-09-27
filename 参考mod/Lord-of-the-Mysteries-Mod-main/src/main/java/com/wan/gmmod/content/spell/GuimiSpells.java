package com.wan.gmmod.content.spell;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.sequences.SequenceRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Set;

/** Registers every active guimi ability as an Iron's spell under the same id. */
public final class GuimiSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, GuimiMod.MODID);

    private GuimiSpells() {}

    public static void register(IEventBus bus) {
        SequenceRegistry.init();
        Set<String> seen = new HashSet<>();
        for (Ability ability : AbilityRegistry.actives()) {
            String path = ability.getId().getPath();
            if (path == null || path.isBlank() || !seen.add(path)) continue;
            Ability captured = ability;
            SPELLS.register(path, () -> new GuimiAbilitySpell(captured));
        }
        SPELLS.register(bus);
    }

    public static AbstractSpell get(ResourceLocation id) {
        if (id == null) return SpellRegistry.none();
        AbstractSpell spell = SpellRegistry.getSpell(id);
        return spell == null ? SpellRegistry.none() : spell;
    }
}