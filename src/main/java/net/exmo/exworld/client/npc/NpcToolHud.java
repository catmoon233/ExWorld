package net.exmo.exworld.client.npc;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.npc.item.NpcWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Ore HUD for the NPC wand. It stays hidden unless the tool is held. */
public final class NpcToolHud {
    private NpcToolHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "npc_tool_hud"), (graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack stack = NpcToolClient.held(minecraft);
        if (stack == null) return;
        int x = 8;
        int y = minecraft.getWindow().getGuiScaledHeight() - 78;
        OreChrome.panel(graphics, x, y, 220, 68, OreChrome.GOLD);
        String name = NpcToolView.name.isBlank() ? Component.translatable("npc.exworld.tool_empty").getString() : NpcToolView.name;
        graphics.drawString(minecraft.font, Component.translatable("item.exworld.npc_wand"), x + 8, y + 8, OreChrome.GOLD, false);
        graphics.drawString(minecraft.font, clip(name, 28), x + 8, y + 20, OreChrome.INK, false);
        String mode = NpcWandItem.MODES[Math.floorMod(NpcWandItem.mode(stack), NpcWandItem.MODES.length)];
        String near = "";
        if (minecraft.player != null && minecraft.level != null) {
            near = NpcToolView.nearest(minecraft.player.position(), minecraft.level.dimension().location().toString(), 8);
        }
        String extra = near.isBlank() ? "" : "  " + near;
        int points = NpcToolView.activePoints();
        if (points > 0) extra += "  " + Component.translatable("npc.exworld.points", points).getString();
        graphics.drawString(minecraft.font, clip(Component.translatable("npc.exworld.mode." + mode).getString() + extra, 32), x + 8, y + 34, OreChrome.MUTED, false);
        graphics.drawString(minecraft.font, Component.translatable("npc.exworld.wand_hint"), x + 8, y + 48, OreChrome.MUTED, false);
    }

    private static String clip(String value, int max) {
        if (value == null || value.length() <= max) return value == null ? "" : value;
        return value.substring(0, max - 1) + "…";
    }
}
