package net.exmo.exworld.character;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class CharacterClientHooks {
    private CharacterClientHooks() {}

    public static void open(String text) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.exmo.exworld.client.character.CharacterClient.open(text);
        }
    }
}
