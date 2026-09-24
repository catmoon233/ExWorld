package net.exmo.lotm.sequence;

import net.neoforged.bus.api.Event;

/** Fired on the game bus during common setup. Addons register pathways here. */
public final class RegisterSequencesEvent extends Event {
    public void register(PathwayDefinition pathway) {
        SequenceRegistry.register(pathway);
    }
}
