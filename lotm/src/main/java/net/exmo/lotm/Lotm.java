package net.exmo.lotm;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Lotm.MODID)
public final class Lotm {
    public static final String MODID = "lotm";

    public Lotm(IEventBus modBus) {
        LotmBootstrap.register(modBus);
    }
}
