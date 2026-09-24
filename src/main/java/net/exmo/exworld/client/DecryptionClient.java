package net.exmo.exworld.client;

import net.exmo.exworld.Config;
import net.exmo.exworld.client.perspective.FirstPersonToggle;
import net.minecraft.client.Minecraft;

/** Applies the server-authored decryption flag on the client that owns the current session. */
public final class DecryptionClient {
    private DecryptionClient() {}

    public static void apply(boolean enabled) {
        Config.decryptionMode = enabled;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        minecraft.execute(() -> {
            Config.decryptionMode = enabled;
            WorldMapClient.applyDeckKey(enabled);
            FirstPersonToggle.enforceDecryption();
        });
    }
}
