package net.exmo.exworld.mystery.blueprint;

import net.exmo.exworld.mystery.MysteryNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** DM edits are validated and stored as drafts; publishing never mutates the current run. */
public final class BlueprintEditorActions {
    private BlueprintEditorActions() {}

    public static void open(ServerPlayer player, String rawId) {
        if (!player.hasPermissions(2)) return;
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !id.getNamespace().equals("exworld")) return;
        BlueprintStore store = BlueprintStore.get(player.server);
        String json = store.drafts.getOrDefault(rawId,
                store.published.getOrDefault(rawId, BlueprintLibrary.defaultJson(rawId)));
        if (json == null) json = "{\"id\":\"" + rawId + "\",\"version\":1,\"nodes\":[{" +
                "\"id\":\"start\",\"type\":\"trigger\",\"params\":{\"kind\":\"on_phase_enter\",\"value\":\"INTRO\"},\"x\":20,\"y\":20}],\"edges\":[]}";
        CompoundTag tag = new CompoundTag(); tag.putString("id", rawId); tag.putString("json", json);
        MysteryNetwork.send(player, "blueprint", tag);
    }

    public static void handle(ServerPlayer player, CompoundTag request) {
        if (!player.hasPermissions(2)) return;
        String kind = request.getString("kind"), id = request.getString("id"), json = request.getString("json");
        String result;
        try {
            if (json.length() > 256_000) throw new IllegalArgumentException("blueprint too large");
            BlueprintGraph graph = "publish".equals(kind) ? BlueprintGraph.parse(json) : BlueprintGraph.parseDraft(json);
            if (!graph.id().toString().equals(id) || !graph.id().getNamespace().equals("exworld"))
                throw new IllegalArgumentException("blueprint id mismatch");
            if ("publish".equals(kind)) graph.validateResources(player.server);
            BlueprintStore store = BlueprintStore.get(player.server);
            if ("save_draft".equals(kind)) {
                store.drafts.put(id, graph.json());
                result = "草稿已保存";
            } else if ("publish".equals(kind)) {
                store.drafts.put(id, graph.json());
                store.published.put(id, graph.json());
                result = "已发布；下一局生效";
            } else throw new IllegalArgumentException("unknown edit action");
            store.changed();
        } catch (RuntimeException error) { result = "校验失败：" + error.getMessage(); }
        CompoundTag tag = new CompoundTag(); tag.putString("message", result);
        MysteryNetwork.send(player, "blueprint_result", tag);
    }
}
