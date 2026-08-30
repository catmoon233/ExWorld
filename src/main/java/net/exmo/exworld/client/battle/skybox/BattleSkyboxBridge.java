package net.exmo.exworld.client.battle.skybox;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Small interface implemented by an optional client-side skybox adapter. */
public interface BattleSkyboxBridge {
    List<ResourceLocation> candidates();

    void activate(ResourceLocation skyboxId);

    void deactivate();

    default void tick() {}
}
