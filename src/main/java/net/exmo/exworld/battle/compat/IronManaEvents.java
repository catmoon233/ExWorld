package net.exmo.exworld.battle.compat;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import net.exmo.exworld.battle.BattleSystem;
import net.neoforged.bus.api.SubscribeEvent;

public final class IronManaEvents {
    private IronManaEvents() {}
    @SubscribeEvent
    public static void onManaChange(ChangeManaEvent event) {
        if (event.getNewMana() > event.getOldMana() && !ManaMutationContext.allowed()
                && BattleSystem.isParticipating(event.getEntity().getUUID())) event.setCanceled(true);
    }
}
