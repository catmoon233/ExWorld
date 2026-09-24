package net.exmo.exworld.client.npc;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.npc.item.NpcWandItem;
import net.exmo.exworld.npc.network.NpcPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Asks the server for geometry when the held wand selection changes. */
public final class NpcToolClient {
    private static String asked = "";
    private static int askedMode = -1;

    private NpcToolClient() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack stack = held(minecraft);
        if (stack == null) {
            asked = "";
            askedMode = -1;
            if (!NpcToolView.id.isBlank()) NpcToolView.clear();
            return;
        }
        String id = NpcWandItem.documentId(stack);
        int mode = NpcWandItem.mode(stack);
         NpcToolView.mode = mode;
         if (id.equals(asked) && mode == askedMode && id.equals(NpcToolView.id)) return;
        asked = id;
        askedMode = mode;
        if (id.isBlank()) {
            NpcToolView.clear();
            return;
        }
        PacketDistributor.sendToServer(new NpcPayloads.Server("tool", new CompoundTag()));
    }

    static ItemStack held(Minecraft minecraft) {
        if (minecraft.player == null) return null;
        ItemStack main = minecraft.player.getMainHandItem();
        if (main.is(ExWorldContent.NPC_WAND.get())) return main;
        ItemStack off = minecraft.player.getOffhandItem();
        return off.is(ExWorldContent.NPC_WAND.get()) ? off : null;
    }
}
