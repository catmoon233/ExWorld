package net.exmo.exkeys.network;

import net.exmo.exkeys.network.ExKeysPayloads.PolicyPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Client entry that the common network registrar can name without loading client classes on a dedicated server. */
public final class ExKeysClientHooks {
    private ExKeysClientHooks() {}

    public static void register() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.exmo.exkeys.client.ExKeysClient.register();
        }
    }

    public static void receive(PolicyPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.exmo.exkeys.client.ExKeysClient.receive(payload);
        }
    }
}
