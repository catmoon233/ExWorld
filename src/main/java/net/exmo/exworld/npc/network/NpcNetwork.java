package net.exmo.exworld.npc.network;

import net.exmo.exworld.npc.NpcSystem;
import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NpcNetwork {
    private NpcNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(NpcPayloads.Client.TYPE, NpcPayloads.Client.STREAM_CODEC,
                (payload, context) -> net.exmo.exworld.client.npc.NpcClient.receive(payload));
        registrar.playToServer(NpcPayloads.Server.TYPE, NpcPayloads.Server.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) NpcSystem.handle(player, payload);
        });
    }

    public static void editor(ServerPlayer player, CompoundTag document, ListTag relations, String ids, String error) {
        CompoundTag tag = new CompoundTag();
        tag.put("document", document);
        tag.put("relations", relations);
        tag.putString("ids", ids == null ? "" : ids);
        tag.putString("error", error == null ? "" : error);
        PacketDistributor.sendToPlayer(player, new NpcPayloads.Client("editor", tag));
    }

    public static void result(ServerPlayer player, boolean ok, String error) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ok", ok);
        tag.putString("error", error == null ? "" : error);
        PacketDistributor.sendToPlayer(player, new NpcPayloads.Client("result", tag));
    }

    public static void dialog(ServerPlayer player, UrbanNpc npc, DialogScript script, String text, boolean thinking) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("entity", npc.getId());
        tag.putString("dialog", script.id());
        tag.putString("name", npc.getName().getString());
        tag.putString("text", text == null ? "" : text);
        tag.putBoolean("thinking", thinking);
        tag.putInt("argb", script.background().argb());
        tag.putFloat("alpha", script.background().alpha());
        tag.putString("texture", script.background().texture());
        tag.putInt("speed", script.typeSpeed());
        StringBuilder buttons = new StringBuilder();
        for (DialogScript.DialogButton button : script.buttons()) {
            if (!buttons.isEmpty()) buttons.append('\n');
            buttons.append(button.label()).append('\t').append(button.dialogId()).append('\t').append(button.actionId());
        }
        tag.putString("buttons", buttons.toString());
        PacketDistributor.sendToPlayer(player, new NpcPayloads.Client("dialog", tag));
    }
}
