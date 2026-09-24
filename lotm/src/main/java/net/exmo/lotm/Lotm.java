package net.exmo.lotm;

import net.exmo.lotm.phone.PhoneSystem;
import net.exmo.lotm.sequence.SequenceClientHooks;
import net.exmo.lotm.sequence.SequenceSystem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Lotm.MODID)
public final class Lotm {
    public static final String MODID = "lotm";

    public Lotm(IEventBus modBus) {
        SequenceSystem.register(modBus);
        SequenceSystem.registerEvents();
        PhoneSystem.register(modBus);
        SequenceClientHooks.registerClient(modBus);
    }
}
