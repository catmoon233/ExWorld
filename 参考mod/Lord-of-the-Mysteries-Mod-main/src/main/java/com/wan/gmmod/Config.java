package com.wan.gmmod;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // 占卜相关配置
    public static final ModConfigSpec.IntValue DIVINATION_SPIRITUALITY_COST = BUILDER
            .comment("占卜所需并消耗的最低灵性值")
            .defineInRange("divinationSpiritualityCost", 10, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DIVINATION_COOLDOWN_SECONDS = BUILDER
            .comment("占卜冷却时间（秒）")
            .defineInRange("divinationCooldownSeconds", 30, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue DIVINATION_REQUIRE_SAFE_STATE = BUILDER
            .comment("是否要求处于安全状态（不在水中/未骑乘/非战斗中）才能占卜")
            .define("divinationRequireSafeState", true);

    // 魔镜占卜（占卜 / 反占卜 / 通灵）相关配置
    public static final ModConfigSpec.IntValue MIRROR_SPIRITUALITY_COST = BUILDER
            .comment("魔镜占卜每种模式消耗的灵性值")
            .defineInRange("mirrorSpiritualityCost", 30, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue MIRROR_COOLDOWN_SECONDS = BUILDER
            .comment("魔镜占卜三种模式共享的冷却时间（秒）")
            .defineInRange("mirrorCooldownSeconds", 60, 0, Integer.MAX_VALUE);

    // 非凡特性（守恒定律）相关配置
    // 各序列等级（0~9）的特性世界初始总量：索引 = 序列等级，
    // index0 = 序列0（最强，数量最少），index9 = 序列9（最弱，数量最多）。
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> CHARACTERISTIC_INITIAL_TOTALS = BUILDER
            .comment("各序列等级(0~9)的非凡特性世界初始总量。索引=序列等级，index0=序列0(最强),index9=序列9(最弱)。")
            .defineListAllowEmpty("characteristicInitialTotals",
                    List.of(1, 2, 4, 8, 15, 30, 60, 100, 150, 200),
                    () -> 0, Config::validateNonNegativeInt);

    public static final ModConfigSpec.BooleanValue CHARACTERISTIC_DROP_ATTRACTION = BUILDER
            .comment("是否启用聚合定律：携带特性的生物死亡时，特性物品飞向附近同/相近途径玩家")
            .define("characteristicDropAttraction", true);

    public static final ModConfigSpec.IntValue CHARACTERISTIC_ATTRACTION_RANGE = BUILDER
            .comment("聚合定律：掉落吸引 / 天使级放牧的搜索半径（方块）")
            .defineInRange("characteristicAttractionRange", 32, 1, 1024);

    public static final ModConfigSpec.DoubleValue CHARACTERISTIC_DEATH_FUSION_CHANCE = BUILDER
            .comment("聚合定律：非凡生物死亡时，特性与附近耐久物品融合成封印物的概率（0=禁用）")
            .defineInRange("characteristicDeathFusionChance", 0.35, 0.0, 1.0);

    // 获取途径多样化：灵界钓鱼 / 非凡集会 / 遗迹固定生成 / 聚合半径成长
    public static final ModConfigSpec.DoubleValue CHAR_FISHING_CHANCE = BUILDER
            .comment("灵界钓鱼：使用灵性钓竿在夜晚垂钓时，钓上低序列非凡特性的概率（0=禁用）")
            .defineInRange("characteristicFishingChance", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue GATHERING_CHANCE = BUILDER
            .comment("非凡集会：流浪商人化身「非凡集会商人」出售非凡特性的出现概率（每次生成判定）")
            .defineInRange("gatheringTraderChance", 0.60, 0.0, 1.0);

    public static final ModConfigSpec.IntValue GATHERING_BASE_PRICE = BUILDER
            .comment("非凡集会：特性交易的基础价格（绿宝石）")
            .defineInRange("gatheringBasePrice", 8, 1, 64);

    public static final ModConfigSpec.IntValue GATHERING_LEVEL_STEP = BUILDER
            .comment("非凡集会：序列每高一级的价格增量（绿宝石）")
            .defineInRange("gatheringLevelStep", 3, 0, 64);

    public static final ModConfigSpec.IntValue GATHERING_SCARCITY_BONUS = BUILDER
            .comment("非凡集会：稀有度加成系数——守恒池中该特性剩余越少，加成越大，价格越贵")
            .defineInRange("gatheringScarcityBonus", 40, 0, 1024);

    public static final ModConfigSpec.BooleanValue GUARANTEED_CHEST_ENABLED = BUILDER
            .comment("遗迹结构：古老神殿 / 古代都市等宝箱必定生成一份非凡特性（守恒池耗尽则跳过）")
            .define("guaranteedChestCharacteristics", true);

    public static final ModConfigSpec.DoubleValue AGGREGATION_GROWTH_PER_ITEM = BUILDER
            .comment("聚合定律：附近每多一份特性/封印物物品，聚合半径的倍率增幅")
            .defineInRange("aggregationGrowthPerItem", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue AGGREGATION_MAX_MULTIPLIER = BUILDER
            .comment("聚合定律：聚合半径可被放大的最大倍率")
            .defineInRange("aggregationMaxMultiplier", 3.0, 1.0, 16.0);

    // 封印物（非凡物品）持有效果
    public static final ModConfigSpec.IntValue SEALED_ARTIFACT_DEMIGOD_THRESHOLD = BUILDER
            .comment("封印物：半神及以上非凡者（序列号 <= 该值，序列4及以上为半神）持有封印物时负面效果减弱")
            .defineInRange("sealedArtifactDemigodThreshold", 4, 0, 9);

    public static final ModConfigSpec.IntValue SEALED_ARTIFACT_DEMIGOD_REDUCTION = BUILDER
            .comment("封印物：半神非凡者持有封印物时，封印侵蚀降低的效果等级数")
            .defineInRange("sealedArtifactDemigodReduction", 1, 0, 4);

    // 实验性途径总开关：默认关闭。关闭时除愚者 / 魔女 / 战争之红外的途径能力
    // 不会解锁、不会 tick、不可触发；测试时可用 /guimi experimental on 临时打开。
    public static final ModConfigSpec.BooleanValue EXPERIMENTAL_PATHWAYS_ENABLED = BUILDER
            .comment("是否启用实验性途径能力（倒吊人/空想家/暴君等 19 条途径，默认关闭，仅供测试）")
            .define("experimentalPathwaysEnabled", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    private static boolean validateNonNegativeInt(final Object obj) {
        return obj instanceof Integer i && i >= 0;
    }
}
