package com.wan.gmmod.client.gui;

import net.minecraft.client.Minecraft;

public final class MirrorDivinationScreenOpener {
    private MirrorDivinationScreenOpener() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new MirrorDivinationScreen());
    }
}