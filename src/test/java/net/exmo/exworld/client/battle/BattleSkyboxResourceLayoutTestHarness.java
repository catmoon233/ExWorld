package net.exmo.exworld.client.battle;

import java.util.List;

/** Verifies that skybox JSON files use the resource-pack path scanned by NeoforgeSkyboxes. */
public final class BattleSkyboxResourceLayoutTestHarness {
    private static final List<String> BIOMES = List.of(
            "prairie", "forest", "swamp", "desert", "badlands", "taiga", "highlands", "flower_fields");

    public static void main(String[] args) {
        for (String biome : BIOMES) {
            String path = "assets/fabricskyboxes/sky/exworld/battle/" + biome + ".json";
            require(BattleSkyboxResourceLayoutTestHarness.class.getClassLoader().getResource(path) != null,
                    "missing skybox resource at " + path);
        }
        System.out.println("BATTLE_SKYBOX_RESOURCE_LAYOUT_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
