package net.exmo.exkeys;

import com.mojang.logging.LogUtils;
import net.exmo.exkeys.network.ExKeysClientHooks;
import net.exmo.exkeys.network.ExKeysNetwork;
import net.exmo.exkeys.server.ExKeysServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ExKeys.MODID)
public final class ExKeys {
    public static final String MODID = "exkeys";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExKeys(IEventBus modBus) {
        modBus.addListener(ExKeysNetwork::register);
        NeoForge.EVENT_BUS.addListener(ExKeysServer::starting);
        NeoForge.EVENT_BUS.addListener(ExKeysServer::login);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ExKeysClientHooks.register();
        }
    }
}
