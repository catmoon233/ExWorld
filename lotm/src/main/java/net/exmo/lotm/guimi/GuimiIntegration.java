package net.exmo.lotm.guimi;

import net.minecraft.server.level.ServerPlayer;

/** Seam between lotm and guimi_mod. Sequence code should call this, not the bridge. */
public final class GuimiIntegration {
    private GuimiIntegration() {}

    public static void install() {
        GuimiPathwayImport.install();
        GuimiSequenceBridge.register();
    }

    public static void reconcile(ServerPlayer player) {
        GuimiSequenceBridge.reconcile(player);
    }

    public static void push(ServerPlayer player) {
        GuimiSequenceBridge.push(player);
    }

    public static boolean cast(ServerPlayer player, String skillId) {
        return GuimiSequenceBridge.cast(player, skillId);
    }
}
