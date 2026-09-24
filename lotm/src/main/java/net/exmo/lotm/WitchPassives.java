package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.sequence.RegisterSequencesEvent;
import net.neoforged.bus.api.SubscribeEvent;

/** Display entries. Speed, fall reduction and trade prices are applied by {@link LotmRuntime}. */
public final class WitchPassives {
    private WitchPassives() {}

    @SubscribeEvent
    public static void register(RegisterSequencesEvent event) {
        register();
    }

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                LotmRuntime.SHADOW,
                "passive.lotm.shadow_affinity",
                "passive.lotm.shadow_affinity.desc",
                "minecraft:phantom_membrane",
                PassiveTrigger.TICK,
                20,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                LotmRuntime.CHARM,
                "passive.lotm.charm",
                "passive.lotm.charm.desc",
                "minecraft:emerald",
                PassiveTrigger.TICK,
                20,
                (player, context) -> false));
    }
}
