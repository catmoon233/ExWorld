package net.exmo.exworld.world.model;

import java.util.Arrays;

/** Story-facing biome profile shared by layout, map presentation and local terrain adaptation. */
public enum WorldBiome {
    PRAIRIE("prairie", "大草原", 0x73945F, 4.8F, "minecraft:plains",
            "药草、谷物、野兽"),
    FOREST("forest", "苍翠林地", 0x496F55, 3.5F, "minecraft:forest",
            "木材、菌菇、药草"),
    SWAMP("swamp", "低语沼泽", 0x526B58, 1.55F, "minecraft:swamp",
            "芦苇、黏土、沼泽药材"),
    DESERT("desert", "赤沙荒漠", 0xB79A65, 3.1F, "minecraft:desert",
            "砂岩、盐、耐旱药草"),
    BADLANDS("badlands", "赤岩台地", 0xA4624D, 2.2F, "minecraft:badlands",
            "铜矿、金矿、红砂"),
    TAIGA("taiga", "寒松林", 0x587369, 2.8F, "minecraft:taiga",
            "云杉、浆果、兽皮"),
    HIGHLANDS("highlands", "风蚀高地", 0x7B7C72, 2.0F, "minecraft:windswept_hills",
            "铁矿、煤炭、石材"),
    FLOWER_FIELDS("flower_fields", "繁花原", 0x7FA36D, 1.8F, "minecraft:flower_forest",
            "花蜜、染料、稀有药草");

    private final String id;
    private final String displayName;
    private final int mapColor;
    private final float spread;
    private final String vanillaBiomeId;
    private final String resources;

    WorldBiome(String id, String displayName, int mapColor, float spread, String vanillaBiomeId,
               String resources) {
        this.id = id;
        this.displayName = displayName;
        this.mapColor = mapColor;
        this.spread = spread;
        this.vanillaBiomeId = vanillaBiomeId;
        this.resources = resources;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int mapColor() { return mapColor; }
    public float spread() { return spread; }
    public String vanillaBiomeId() { return vanillaBiomeId; }
    public String resources() { return resources; }

    public static WorldBiome byId(String id) {
        return Arrays.stream(values()).filter(profile -> profile.id.equals(id)).findFirst().orElse(PRAIRIE);
    }
}
