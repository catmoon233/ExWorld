package net.exmo.exworld.battle.compat;

import net.neoforged.fml.ModList;

/** Safe presence check that does not link against StylizedDamage classes. */
public final class StylizedDamagePresence {
    private StylizedDamagePresence() {}

    public static boolean active() {
        return ModList.get().isLoaded("stylizeddamage");
    }
}
