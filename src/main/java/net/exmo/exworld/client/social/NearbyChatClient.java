package net.exmo.exworld.client.social;

import net.minecraft.client.Minecraft;

/** Applies a server request to open the nearby-chat configuration screen. */
public final class NearbyChatClient {
    private NearbyChatClient() {}

    public static void open(int distance, int range) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        minecraft.execute(() -> minecraft.setScreen(new NearbyChatScreen(distance, range)));
    }
}
