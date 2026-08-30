package net.exmo.exworld.client.battle.skybox;

import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional adapter for NeoforgeSkyboxes. It is loaded reflectively so the
 * client still works when the optional mod is not installed.
 */
public final class NeoforgeSkyboxesBattleBridge implements BattleSkyboxBridge {
    private static final String MOD_ID = "neoforgeskyboxes";
    private static final String BATTLE_PREFIX = "sky/exworld/battle/";

    private ResourceLocation activeSkybox;
    private List<Skybox> previousActiveSkyboxes;

    @Override
    public List<ResourceLocation> candidates() {
        SkyboxManager manager = manager();
        if (manager == null) return List.of();
        return manager.getSkyboxMap().keySet().stream()
                .filter(NeoforgeSkyboxesBattleBridge::isBattleSkybox)
                .toList();
    }

    @Override
    public void activate(ResourceLocation skyboxId) {
        SkyboxManager manager = manager();
        if (manager == null) return;
        Skybox selected = manager.getSkyboxMap().get(skyboxId);
        if (selected == null) return;

        if (previousActiveSkyboxes == null) previousActiveSkyboxes = new ArrayList<>(manager.getActiveSkyboxes());
        activeSkybox = skyboxId;
        manager.getActiveSkyboxes().clear();
        manager.getActiveSkyboxes().add(selected);
    }

    @Override
    public void deactivate() {
        SkyboxManager manager = manager();
        if (manager != null) {
            manager.getActiveSkyboxes().clear();
            if (previousActiveSkyboxes != null) manager.getActiveSkyboxes().addAll(previousActiveSkyboxes);
        }
        activeSkybox = null;
        previousActiveSkyboxes = null;
    }

    @Override
    public void tick() {
        if (activeSkybox == null) return;
        SkyboxManager manager = manager();
        if (manager == null) return;
        Skybox selected = manager.getSkyboxMap().get(activeSkybox);
        if (selected == null) return;
        manager.getActiveSkyboxes().removeIf(skybox -> skybox != selected);
        if (!manager.getActiveSkyboxes().contains(selected)) manager.getActiveSkyboxes().add(selected);
    }

    private static boolean isBattleSkybox(ResourceLocation id) {
        return id.getNamespace().equals("fabricskyboxes")
                && id.getPath().startsWith(BATTLE_PREFIX)
                && id.getPath().endsWith(".json");
    }

    private static SkyboxManager manager() {
        try {
            return ModList.get().isLoaded(MOD_ID) ? SkyboxManager.getInstance() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
