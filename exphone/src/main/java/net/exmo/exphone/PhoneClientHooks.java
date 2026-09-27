package net.exmo.exphone;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Keeps client-only phone screens out of dedicated-server execution. */
public final class PhoneClientHooks {
    private PhoneClientHooks() {}

    public static void receive(PhonePayloads.PhoneStatePayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) PhoneClient.state(payload.json());
    }
}
