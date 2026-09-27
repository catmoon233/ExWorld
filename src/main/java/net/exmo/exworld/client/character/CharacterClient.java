package net.exmo.exworld.client.character;

import net.minecraft.client.Minecraft;

public final class CharacterClient {
    private CharacterClient() {}

    public static void open(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.setScreen(new CharacterScreen(text == null ? "" : text));
    }
}
