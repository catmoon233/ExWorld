package net.exmo.exworld.client.npc;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.npc.network.NpcPayloads;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class NpcClient {
    private NpcClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NpcClient::renderers);
        modBus.addListener(NpcToolHud::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(NpcToolWorld::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(NpcToolClient::tick);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExWorldContent.URBAN_NPC.get(), UrbanNpcRenderer::new);
    }

    public static void receive(NpcPayloads.Client payload) {
        if (payload == null || payload.tag() == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            switch (payload.kind()) {
                case "editor" -> minecraft.setScreen(new NpcEditorScreen(payload.tag()));
                case "blueprint" -> minecraft.setScreen(new NpcBlueprintScreen(payload.tag()));
                case "overlay" -> NpcToolView.install(payload.tag());
                case "result" -> {
                    if (minecraft.screen instanceof NpcEditorScreen editor) editor.result(payload.tag().getBoolean("ok"), payload.tag().getString("error"));
                    if (minecraft.screen instanceof NpcBlueprintScreen blueprint) blueprint.result(payload.tag().getBoolean("ok"), payload.tag().getString("error"));
                }
                case "dialog" -> {
                    if (minecraft.screen instanceof UrbanDialogScreen dialog && dialog.same(payload.tag().getInt("entity"))) dialog.update(payload.tag());
                    else minecraft.setScreen(new UrbanDialogScreen(payload.tag()));
                }
                default -> {}
            }
        });
    }
}
