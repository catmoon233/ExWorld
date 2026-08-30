package net.exmo.exworld.battle.api;

import java.util.Optional;
import net.neoforged.bus.api.IEventBus;

/** The deliberately small battle seam consumed by maps, events, commands and tests. */
public interface BattleInterface {
    BattleId startEncounter(EncounterRequest request);
    CommandReceipt submit(BattleCommand command);
    Optional<BattleSnapshot> snapshot(BattleId battleId);
    /** Native NeoForge bus used by all active battle sessions. */
    IEventBus events();
}
