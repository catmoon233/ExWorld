package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.BasicPoisonItem;
import com.wan.gmmod.common.item.BlankTalismanItem;
import com.wan.gmmod.common.item.BlackShoesItem;
import com.wan.gmmod.common.item.BlackVestItem;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.common.item.ChargedFireballItem;
import com.wan.gmmod.common.item.DawnArmorItem;
import com.wan.gmmod.common.item.DawnSwordItem;
import com.wan.gmmod.common.item.MagmaSwordItem;
import com.wan.gmmod.common.item.DemonFormItem;
import com.wan.gmmod.common.item.FlameWeaponItem;
import com.wan.gmmod.common.item.GuimiArmorItem;
import com.wan.gmmod.common.item.HemostaticSalveItem;
import com.wan.gmmod.common.item.SealedArtifactItem;
import com.wan.gmmod.common.item.SeerPotionItem;
import com.wan.gmmod.common.item.LongPantsItem;
import com.wan.gmmod.common.item.MagicArtifactItem;
import com.wan.gmmod.common.item.MirrorItem;
import com.wan.gmmod.common.item.PendulumItem;
import com.wan.gmmod.common.item.SpiritRodItem;
import com.wan.gmmod.common.item.VestItem;
import com.wan.gmmod.common.item.WandItem;
import com.wan.gmmod.common.item.WolfmanFormItem;
import com.wan.gmmod.content.talisman.TalismanData;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // 创建一个 DeferredRegister 用于物品
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(GuimiMod.MODID);


    public static final DeferredItem<SeerPotionItem> SEER_POTION =
           ITEMS.register("seer_potion",
                    () -> new SeerPotionItem(new Item.Properties(),
                            GuimiMod.id("fool_9")));

    public static final DeferredItem<GuimiArmorItem> TOP_HAT =
            ITEMS.register("top_hat",
                    () -> new GuimiArmorItem(ModArmorMaterials.TOP_HAT, ArmorItem.Type.HELMET,
                            new Item.Properties().stacksTo(1)));

    public static final DeferredItem<WandItem> WAND =
            ITEMS.register("wand",
                    () -> new WandItem(new Item.Properties().stacksTo(1)));

    /**
     * 黄水晶灵摆，右键使用时通过网络包驱动客户端动画与玩家手臂旋转，
     * 使用 GeckoLib 渲染 3D 模型
     */
    public static final DeferredItem<PendulumItem> PENDULUM =
            ITEMS.register("pendulum",
                    () -> new PendulumItem(new Item.Properties().stacksTo(1)));

    /**
     * 马甲盔甲，装填于胸甲槽，使用 GeckoLib 渲染 3D 模型
     */
    public static final DeferredItem<VestItem> VEST =
            ITEMS.register("vest",
                    () -> new VestItem(ModArmorMaterials.VEST, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)));

    /**
     * 黑色长裤盔甲，装填于护腿槽，使用 GeckoLib 渲染 3D 模型
     */
    public static final DeferredItem<LongPantsItem> LONG_PANTS =
            ITEMS.register("long_pants",
                    () -> new LongPantsItem(ModArmorMaterials.LONG_PANTS, ArmorItem.Type.LEGGINGS,
                            new Item.Properties().stacksTo(1)));

    /**
     * 黑皮鞋盔甲，装填于靴子槽，使用 GeckoLib 渲染 3D 模型
     */
    public static final DeferredItem<BlackShoesItem> BLACK_SHOES =
            ITEMS.register("black_shoes",
                    () -> new BlackShoesItem(ModArmorMaterials.BLACK_SHOES, ArmorItem.Type.BOOTS,
                            new Item.Properties().stacksTo(1)));

    /**
     * 黑色马甲（白衬）盔甲，装填于胸甲槽，使用 GeckoLib 渲染 3D 模型
     */
    public static final DeferredItem<BlackVestItem> BLACK_VEST =
            ITEMS.register("black_vest",
                    () -> new BlackVestItem(ModArmorMaterials.BLACK_VEST, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)));

    /**
     * 怨灵刷怪蛋
     */
    public static final DeferredItem<DeferredSpawnEggItem> WRAITH_SPAWN_EGG =
            ITEMS.register("wraith_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.WRAITH,
                            0x3a2a4d, 0x8a6fb0, new Item.Properties()));

    /**
     * 美人鱼刷怪蛋
     */
    public static final DeferredItem<DeferredSpawnEggItem> MERMAID_SPAWN_EGG =
            ITEMS.register("mermaid_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.MERMAID,
                            0x1f6f8b, 0xf2c14e, new Item.Properties()));

    /**
     * 修女刷怪蛋
     */
    public static final DeferredItem<DeferredSpawnEggItem> NUN_SPAWN_EGG =
            ITEMS.register("nun_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.NUN,
                            0x2b2f3d, 0x9aa4b8, new Item.Properties()));

    /**
     * 神父刷怪蛋
     */
    public static final DeferredItem<DeferredSpawnEggItem> PRIEST_SPAWN_EGG =
            ITEMS.register("priest_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.PRIEST,
                            0xe6e8eb, 0x5a6472, new Item.Properties()));

    /** 狼人刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> WOLFMAN_SPAWN_EGG =
            ITEMS.register("wolfman_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.WOLFMAN,
                            0x3d3024, 0x8a7355, new Item.Properties()));

    // ========== 新增生物刷怪蛋 ==========

    /** 人皮幽影刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> HUMAN_SKIN_SHADOW_SPAWN_EGG =
            ITEMS.register("human_skin_shadow_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.HUMAN_SKIN_SHADOW,
                            0x8a7a6a, 0x3a2a4d, new Item.Properties()));

    /** 邪纹黑豹刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> EVIL_PANTHER_SPAWN_EGG =
            ITEMS.register("evil_panther_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.EVIL_PANTHER,
                            0x1a1a2e, 0x4a6a3a, new Item.Properties()));

    /** 千面狩猎者刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> THOUSAND_FACED_HUNTER_SPAWN_EGG =
            ITEMS.register("thousand_faced_hunter_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.THOUSAND_FACED_HUNTER,
                            0x8b4513, 0x4a280a, new Item.Properties()));

    /** 白尾赤狐刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> WHITE_FOX_SPAWN_EGG =
            ITEMS.register("white_fox_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.WHITE_FOX,
                            0xf5e6d0, 0xd4a574, new Item.Properties()));

    /** 寡妇巨蛛刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> WIDOW_SPIDER_SPAWN_EGG =
            ITEMS.register("widow_spider_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.WIDOW_SPIDER,
                            0x2a1a0a, 0x6a3a1a, new Item.Properties()));

    /** 霍纳奇斯灰山羊刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> HORNACHIS_GOAT_SPAWN_EGG =
            ITEMS.register("hornachis_goat_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.HORNACHIS_GOAT,
                            0x8a8a8a, 0x5a5a5a, new Item.Properties()));

    /** 岩浆之魔刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> LAVA_DEMON_SPAWN_EGG =
            ITEMS.register("lava_demon_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.LAVA_DEMON,
                            0xff4500, 0x8b0000, new Item.Properties()));

    /** 极光会-K先生刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> MR_K_SPAWN_EGG =
            ITEMS.register("mr_k_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.MR_K,
                            0x2a3a5a, 0x8a9aba, new Item.Properties()));

    /** 魔女教会-布朗丝·索伦刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> BROWN_SILK_SOLEN_SPAWN_EGG =
            ITEMS.register("brown_silk_solen_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.BROWN_SILK_SOLEN,
                            0x4a2a5a, 0xba8aba, new Item.Properties()));

    /** 深渊恶魔刷怪蛋 */
    public static final DeferredItem<DeferredSpawnEggItem> ABYSS_DEMON_SPAWN_EGG =
            ITEMS.register("abyss_demon_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ABYSS_DEMON,
                            0x1a0a2a, 0x6a2a8a, new Item.Properties()));

    // ===== 新增13个生物刷怪蛋 =====
    public static final DeferredItem<DeferredSpawnEggItem> EVIL_BLACK_CAT_SPAWN_EGG =
            ITEMS.register("evil_black_cat_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.EVIL_BLACK_CAT, 0x1a1a1a, 0x8a6a3a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DEATH_RAVEN_SPAWN_EGG =
            ITEMS.register("death_raven_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.DEATH_RAVEN, 0x1a1a2a, 0x4a3a2a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> RAIN_BIRD_SPAWN_EGG =
            ITEMS.register("rain_bird_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.RAIN_BIRD, 0x3a6a8a, 0x8abada, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> NIGHTMARE_SHADOW_SPAWN_EGG =
            ITEMS.register("nightmare_shadow_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.NIGHTMARE_SHADOW, 0x2a1a3a, 0x6a4a8a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> VENGEFUL_SHADOW_SPAWN_EGG =
            ITEMS.register("vengeful_shadow_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.VENGEFUL_SHADOW, 0x3a2a1a, 0x7a5a3a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LIVING_CORPSE_SPAWN_EGG =
            ITEMS.register("living_corpse_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.LIVING_CORPSE, 0x5a4a3a, 0x8a7a6a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FIRE_SALAMANDER_SPAWN_EGG =
            ITEMS.register("fire_salamander_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.FIRE_SALAMANDER, 0xff4a00, 0x8a1a00, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GRAY_BIRD_GRANDMA_SPAWN_EGG =
            ITEMS.register("gray_bird_grandma_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.GRAY_BIRD_GRANDMA, 0x8a7a6a, 0x5a4a3a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ONE_EYED_BULL_SPAWN_EGG =
            ITEMS.register("one_eyed_bull_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ONE_EYED_BULL, 0xdaa87a, 0x8a6a4a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ROTTEN_SHEPHERD_SPAWN_EGG =
            ITEMS.register("rotten_shepherd_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ROTTEN_SHEPHERD, 0x4a3a2a, 0x7a5a4a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BLACK_SPOTTED_FROG_SPAWN_EGG =
            ITEMS.register("black_spotted_frog_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.BLACK_SPOTTED_FROG, 0x2a5a1a, 0x8aba4a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FROG_MEAT_PUPPET_SPAWN_EGG =
            ITEMS.register("frog_meat_puppet_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.FROG_MEAT_PUPPET, 0x3a4a2a, 0x6a8a5a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BLACK_SCALE_SHARK_SPAWN_EGG =
            ITEMS.register("black_scale_shark_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.BLACK_SCALE_SHARK, 0x1a2a3a, 0x4a6a8a, new Item.Properties()));

    // ===== 配方材料来源生物刷怪蛋（新增）=====
    public static final DeferredItem<DeferredSpawnEggItem> SILVER_WAR_BEAR_SPAWN_EGG =
            ITEMS.register("silver_war_bear_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.SILVER_WAR_BEAR, 0xd8d8e0, 0x8a8a96, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SKINLESS_BLOOD_CAT_SPAWN_EGG =
            ITEMS.register("skinless_blood_cat_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.SKINLESS_BLOOD_CAT, 0x8a2a2a, 0xd8d8d8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ADULT_UNICORN_SPAWN_EGG =
            ITEMS.register("adult_unicorn_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ADULT_UNICORN, 0xf0f0f5, 0xe8d444, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ADULT_PEGASUS_SPAWN_EGG =
            ITEMS.register("adult_pegasus_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.ADULT_PEGASUS, 0xe8e8f0, 0x88aadd, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DAWN_ROOSTER_SPAWN_EGG =
            ITEMS.register("dawn_rooster_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.DAWN_ROOSTER, 0xe8b84a, 0xc94a2a, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> NIGHTMARE_EYE_SPAWN_EGG =
            ITEMS.register("nightmare_eye_spawn_egg",
                    () -> new DeferredSpawnEggItem(ModEntities.NIGHTMARE_EYE, 0x2a1a3a, 0xe0491f, new Item.Properties()));

    /**
     * 非凡特性物品：所有途径 / 等级共用，靠数据组件区分。参见 {@link CharacteristicItem}。
     */
    public static final DeferredItem<CharacteristicItem> CHARACTERISTIC =
            ITEMS.register("characteristic",
                    () -> new CharacteristicItem(new Item.Properties()));

    /**
     * 封印物物品：锻造台将「特性 + 耐久装备」锻造成封印物，或非凡生物死亡时
     * 与附近物品融合而成。携带途径对应的正面效果与「封印侵蚀」代价。
     * 所有途径 / 等级 / 基底共用，靠数据组件区分。参见 {@link SealedArtifactItem}。
     */
    public static final DeferredItem<SealedArtifactItem> SEALED_ARTIFACT =
            ITEMS.register("sealed_artifact",
                    () -> new SealedArtifactItem(new Item.Properties()));

    /**
     * 神奇物品：将非凡特性「植入」基础物品后形成的超凡物品，携带途径对应的正面
     * 能力与负面代价，且效果具有随机性（生成时掷定变体）。右键触发各自能力。
     * 所有途径 / 等级 / 基底共用，靠数据组件区分。参见 {@link MagicArtifactItem}。
     */
    public static final DeferredItem<MagicArtifactItem> MAGIC_ARTIFACT =
            ITEMS.register("magic_artifact",
                    () -> new MagicArtifactItem(new Item.Properties()));

    // ===== 炼药材料 =====

    /** 星水晶：占卜家魔药配方材料。 */
    public static final DeferredItem<Item> STAR_CRYSTAL =
            ITEMS.register("star_crystal", () -> new Item(new Item.Properties()));

    /** 拉瓦章鱼血液：由原版章鱼掉落，占卜家魔药主料。 */
    public static final DeferredItem<Item> LAVA_OCTOPUS_BLOOD =
            ITEMS.register("lava_octopus_blood", () -> new Item(new Item.Properties()));

    /** 净水（纯水）：水瓶在熔炉烧炼而成，是炼药锅的基底介质。 */
    public static final DeferredItem<Item> PURIFIED_WATER =
            ITEMS.register("purified_water", () -> new Item(new Item.Properties()));

    /** 金薄荷叶子：由金薄荷植物破坏掉落。 */
    public static final DeferredItem<Item> GOLD_MINT_LEAF =
            ITEMS.register("gold_mint_leaf", () -> new Item(new Item.Properties()));

    // ===== 仪式 / 武器 =====

    /** 仪式匕首：激活灵性之墙的必要工具。 */
    public static final DeferredItem<Item> RITUAL_DAGGER =
            ITEMS.register("ritual_dagger",
                    () -> new com.wan.gmmod.common.item.RitualDaggerItem(new Item.Properties().stacksTo(1)));

    /** 丧钟（原名寂灭）：带换弹动画的火器武器，弹药为各类子弹。 */
    public static final DeferredItem<Item> SILENCE_GUN =
            ITEMS.register("silence_gun",
                    () -> new com.wan.gmmod.common.item.SilenceGunItem(new Item.Properties().stacksTo(1).durability(512)));

    /** 纸人：右键地面放置纸人实体，作为「纸人替身」能力的交换锚点。 */
    public static final DeferredItem<Item> PAPER_FIGURINE =
            ITEMS.register("paper_figurine",
                    () -> new com.wan.gmmod.common.item.PaperFigurineItem(new Item.Properties()));

    /** 纸牌：小丑「飞牌」能力的专属弹药，与原版纸通用（优先消耗纸牌）。 */
    public static final DeferredItem<Item> PAPER_CARD =
            ITEMS.register("paper_card", () -> new Item(new Item.Properties()));

    // ===== 子弹类 =====

    /** 普通子弹（基础弹药）。 */
    public static final DeferredItem<Item> BULLET =
            ITEMS.register("bullet", () -> new Item(new Item.Properties()));

    /** 剥夺子弹：命中后剥夺目标一项增益效果。 */
    public static final DeferredItem<Item> DEPRIVATION_BULLET =
            ITEMS.register("deprivation_bullet", () -> new Item(new Item.Properties()));

    /** 寄生子弹：命中后对目标施加寄生效果。 */
    public static final DeferredItem<Item> PARASITIC_BULLET =
            ITEMS.register("parasitic_bullet", () -> new Item(new Item.Properties()));

    /** 控灵子弹：命中灵体时造成额外伤害。 */
    public static final DeferredItem<Item> SPIRIT_CONTROL_BULLET =
            ITEMS.register("spirit_control_bullet", () -> new Item(new Item.Properties()));

    /** 欺瞒子弹：命中后使目标短暂迷惑。 */
    public static final DeferredItem<Item> DECEPTION_BULLET =
            ITEMS.register("deception_bullet", () -> new Item(new Item.Properties()));

    /** 驱邪子弹：对不死生物和灵体造成额外伤害。 */
    public static final DeferredItem<Item> EXORCISM_BULLET =
            ITEMS.register("exorcism_bullet", () -> new Item(new Item.Properties()));

    /** 净化子弹：命中后清除目标负面灵性状态。 */
    public static final DeferredItem<Item> PURIFICATION_BULLET =
            ITEMS.register("purification_bullet", () -> new Item(new Item.Properties()));

    // ===== 魔药 =====

    /** 序列8 - 小丑魔药。 */
    public static final DeferredItem<SeerPotionItem> CLOWN_POTION =
            ITEMS.register("clown_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("fool_8")));

    /** 序列7 - 魔术师魔药。 */
    public static final DeferredItem<SeerPotionItem> MAGICIAN_POTION =
            ITEMS.register("magician_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("fool_7")));

    /** 序列6 - 无面人魔药（炼药系统合成，饮用后正常晋升）。 */
    public static final DeferredItem<SeerPotionItem> FACELESS_POTION =
            ITEMS.register("faceless_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("fool_6")));

    /**
     * 序列5 - 秘偶大师魔药：高序列魔药，服用需满足「美人鱼的歌声」晋升仪式条件，
     * 否则失控。见 {@link com.wan.gmmod.content.ritual.PromotionRitualManager}。
     */
    public static final DeferredItem<Item> MARIONETTIST_POTION =
            ITEMS.register("marionettist_potion",
                    () -> new com.wan.gmmod.common.item.RitualAdvanceItem(new Item.Properties(),
                            GuimiMod.id("fool_5")));

    /** 秘偶大师非凡特性：可作为魔药的替代品右键触发同样的晋升仪式。 */
    public static final DeferredItem<Item> MARIONETTIST_CHARACTERISTIC =
            ITEMS.register("marionettist_characteristic",
                    () -> new com.wan.gmmod.common.item.RitualAdvanceItem(
                            new Item.Properties().fireResistant(), GuimiMod.id("fool_5")));

    /** 无面人非凡特性。 */
    public static final DeferredItem<Item> FACELESS_CHARACTERISTIC =
            ITEMS.register("faceless_characteristic",
                    () -> new Item(new Item.Properties().fireResistant()));

    // ===== 炼药 / 仪式材料 =====

    /** 龙牙草。 */
    public static final DeferredItem<Item> AGRIMONY =
            ITEMS.register("agrimony", () -> new Item(new Item.Properties()));

    /** 黑边太阳花。 */
    public static final DeferredItem<Item> BLACK_EDGED_SUNFLOWER =
            ITEMS.register("black_edged_sunflower", () -> new Item(new Item.Properties()));

    /** 黑色曼陀罗。 */
    public static final DeferredItem<Item> BLACK_MANDRAGORA =
            ITEMS.register("black_mandragora", () -> new Item(new Item.Properties()));

    /** 金斗蓬草。 */
    public static final DeferredItem<Item> GOLDEN_CLOAK_GRASS =
            ITEMS.register("golden_cloak_grass", () -> new Item(new Item.Properties()));

    /** 曼陀罗。 */
    public static final DeferredItem<Item> MANDRAGORA =
            ITEMS.register("mandragora", () -> new Item(new Item.Properties()));

    /** 迷幻草。 */
    public static final DeferredItem<Item> PSYCHEDELIC_GRASS =
            ITEMS.register("psychedelic_grass", () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("psychedelic_grass_plant").get(), new Item.Properties()));

    /** 迷雾树人的真实根茎。 */
    public static final DeferredItem<Item> MIST_TREANT_ROOT =
            ITEMS.register("mist_treant_root", () -> new Item(new Item.Properties()));

    /** 迷雾树人的汁液。 */
    public static final DeferredItem<Item> MIST_TREANT_JUICE =
            ITEMS.register("mist_treant_juice", () -> new Item(new Item.Properties()));

    /** 千面狩猎者的血液。 */
    public static final DeferredItem<Item> THOUSAND_FACED_HUNTER_BLOOD =
            ITEMS.register("thousand_faced_hunter_blood", () -> new Item(new Item.Properties()));

    /** 千面狩猎者异变的脑垂体。 */
    public static final DeferredItem<Item> THOUSAND_FACED_HUNTER_PITUITARY =
            ITEMS.register("thousand_faced_hunter_pituitary", () -> new Item(new Item.Properties()));

    /** 人脸玫瑰。 */
    public static final DeferredItem<Item> FACE_ROSE =
            ITEMS.register("face_rose", () -> new Item(new Item.Properties()));

    /** 人皮幽影特性。 */
    public static final DeferredItem<Item> HUMAN_SKIN_SHADOW_CHARACTERISTIC =
            ITEMS.register("human_skin_shadow_characteristic", () -> new Item(new Item.Properties()));

    /** 成年的霍纳奇斯灰山羊独角结晶。 */
    public static final DeferredItem<Item> HORNACIS_GOAT_HORN_CRYSTAL =
            ITEMS.register("hornacis_goat_horn_crystal", () -> new Item(new Item.Properties()));

    /** 深海娜迦头发。 */
    public static final DeferredItem<Item> DEEP_SEA_NAGA_HAIR =
            ITEMS.register("deep_sea_naga_hair", () -> new Item(new Item.Properties()));

    /** 水形宝石。 */
    public static final DeferredItem<Item> WATER_SHAPE_GEM =
            ITEMS.register("water_shape_gem", () -> new Item(new Item.Properties()));

    /** 邪纹黑豹的脊髓液。 */
    public static final DeferredItem<Item> EVIL_PANTHER_SPINAL_FLUID =
            ITEMS.register("evil_panther_spinal_fluid", () -> new Item(new Item.Properties()));

    // ===== 序列5「秘偶大师」炼药材料 =====

    /** 古老怨灵的粉尘。 */
    public static final DeferredItem<Item> ANCIENT_WRAITH_DUST =
            ITEMS.register("ancient_wraith_dust", () -> new Item(new Item.Properties()));

    /** 六翼石像鬼的核心结晶。 */
    public static final DeferredItem<Item> SIX_WINGED_GARGOYLE_CORE_CRYSTAL =
            ITEMS.register("six_winged_gargoyle_core_crystal", () -> new Item(new Item.Properties()));

    /** 苏尼亚岛金色泉的泉水。 */
    public static final DeferredItem<Item> SONIA_GOLDEN_SPRING_WATER =
            ITEMS.register("sonia_golden_spring_water", () -> new Item(new Item.Properties()));

    /** 龙纹树的树皮。 */
    public static final DeferredItem<Item> DRAGON_PATTERN_TREE_BARK =
            ITEMS.register("dragon_pattern_tree_bark", () -> new Item(new Item.Properties()));

    /** 古老怨灵的残余灵性。 */
    public static final DeferredItem<Item> ANCIENT_WRAITH_RESIDUAL_SPIRITUALITY =
            ITEMS.register("ancient_wraith_residual_spirituality", () -> new Item(new Item.Properties()));

    /** 六翼石像鬼的眼睛。 */
    public static final DeferredItem<Item> SIX_WINGED_GARGOYLE_EYE =
            ITEMS.register("six_winged_gargoyle_eye", () -> new Item(new Item.Properties()));

    // ===== 植物方块物品（BlockItem）=====

    public static final DeferredItem<BlockItem> GOLD_MINT =
            ITEMS.register("gold_mint",
                    () -> new BlockItem(ModBlocks.GOLD_MINT.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> NIGHT_FRAGRANCE =
            ITEMS.register("night_fragrance",
                    () -> new BlockItem(ModBlocks.NIGHT_FRAGRANCE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> DRAGON_BLOOD_GRASS =
            ITEMS.register("dragon_blood_grass",
                    () -> new BlockItem(ModBlocks.DRAGON_BLOOD_GRASS.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> POISON_HEMLOCK =
            ITEMS.register("poison_hemlock",
                    () -> new BlockItem(ModBlocks.POISON_HEMLOCK.get(), new Item.Properties()));

    // ===== 祭台方块物品 =====

    public static final DeferredItem<BlockItem> ALTAR =
            ITEMS.register("altar",
                    () -> new BlockItem(ModBlocks.ALTAR.get(), new Item.Properties()));

    // ===== 魔女途径（刺客 / 教唆者 / 女巫 / 欢愉魔女）=====

    /** 镜子：女巫「魔镜占卜」入口 + 潜行右键绑定「镜子替身」锚点。 */
    public static final DeferredItem<MirrorItem> MIRROR =
            ITEMS.register("mirror",
                    () -> new MirrorItem(new Item.Properties().stacksTo(1)));

    /** 破碎的镜子：镜子碎裂后的残片，作为收集品/魔女途径素材。 */
    public static final DeferredItem<Item> MIRROR_BROKEN =
            ITEMS.register("mirror_broken",
                    () -> new Item(new Item.Properties().stacksTo(16)));

    /** 灵性钓竿：灵界钓鱼专用，夜晚垂钓有概率从守恒池抽出低序列非凡特性。 */
    public static final DeferredItem<SpiritRodItem> SPIRIT_ROD =
            ITEMS.register("spirit_rod",
                    () -> new SpiritRodItem(new Item.Properties().durability(384)));

    /** 序列9 - 刺客魔药。 */
    public static final DeferredItem<SeerPotionItem> ASSASSIN_POTION =
            ITEMS.register("assassin_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("witch_9")));

    /** 序列8 - 教唆者魔药。 */
    public static final DeferredItem<SeerPotionItem> INSTIGATOR_POTION =
            ITEMS.register("instigator_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("witch_8")));

    /** 序列7 - 女巫魔药。 */
    public static final DeferredItem<SeerPotionItem> WITCH_POTION =
            ITEMS.register("witch_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("witch_7")));

    /** 序列6 - 欢愉魔女魔药。 */
    public static final DeferredItem<SeerPotionItem> JOYFUL_WITCH_POTION =
            ITEMS.register("joyful_witch_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("witch_6")));

    // ===== 战争之红途径（猎人 / 挑衅者 / 纵火家 / 阴谋家）=====

    /** 止血药膏：猎人「野外知识」产物，右键瞬间恢复 2 颗心。 */
    public static final DeferredItem<HemostaticSalveItem> HEMOSTATIC_SALVE =
            ITEMS.register("hemostatic_salve",
                    () -> new HemostaticSalveItem(new Item.Properties()));

    /** 基础毒药：猎人「野外知识」产物，涂抹武器使近战附带中毒 5 秒。 */
    public static final DeferredItem<BasicPoisonItem> BASIC_POISON =
            ITEMS.register("basic_poison",
                    () -> new BasicPoisonItem(new Item.Properties()));

    /** 火焰武器：纵火家「火焰武器」凝聚的临时武器（剑 / 鞭 / 马刀）。 */
    public static final DeferredItem<FlameWeaponItem> FLAME_WEAPON =
            ITEMS.register("flame_weapon",
                    () -> new FlameWeaponItem(new Item.Properties().stacksTo(1)));

    // ===== 火焰弹体外观物品（仅供投射物渲染使用自定义纹理，不进创造页）=====

    /** 凝聚火球：「火球术」蓄力时握于手中右键释放，兼作弹体纹理载体。 */
    public static final DeferredItem<Item> FLAME_ORB_ITEM =
            ITEMS.register("flame_orb",
                    () -> new ChargedFireballItem(new Item.Properties().stacksTo(1), false));

    /** 巨大凝聚火球：「巨大火球」蓄力时握于手中右键释放，兼作弹体纹理载体。 */
    public static final DeferredItem<Item> GIANT_FLAME_ORB_ITEM =
            ITEMS.register("giant_flame_orb",
                    () -> new ChargedFireballItem(new Item.Properties().stacksTo(1), true));

    /** 火鸦外观：「火鸦术」弹体纹理载体。 */
    public static final DeferredItem<Item> FIRE_RAVEN_ITEM =
            ITEMS.register("fire_raven", () -> new Item(new Item.Properties()));

    /** 炽白之枪外观：「炽白之枪」弹体纹理载体。 */
    public static final DeferredItem<Item> FLAME_SPEAR_ITEM =
            ITEMS.register("flame_spear", () -> new Item(new Item.Properties()));

    // ===== 魔女途径弹体外观物品（仅投射物渲染用，不进创造页）=====

    /** 黑焰外观：「操控黑焰」弹体的可见载体（黑色火焰贴图）。 */
    public static final DeferredItem<Item> BLACK_FLAME_ITEM =
            ITEMS.register("black_flame", () -> new Item(new Item.Properties()));

    /** 冰晶长枪外观：投掷时可看到冰晶长枪纹理发射出去。 */
    public static final DeferredItem<Item> ICE_SPEAR_ITEM =
            ITEMS.register("ice_spear", () -> new Item(new Item.Properties()));

    /** 序列9 - 猎人魔药。 */
    public static final DeferredItem<SeerPotionItem> HUNTER_POTION =
            ITEMS.register("hunter_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("war_9")));

    /** 序列8 - 挑衅者魔药。 */
    public static final DeferredItem<SeerPotionItem> PROVOKER_POTION =
            ITEMS.register("provoker_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("war_8")));

    /** 序列7 - 纵火家魔药。 */
    public static final DeferredItem<SeerPotionItem> PYROMANIAC_POTION =
            ITEMS.register("pyromaniac_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("war_7")));

    /** 序列6 - 阴谋家魔药。 */
    public static final DeferredItem<SeerPotionItem> CONSPIRER_POTION =
            ITEMS.register("conspirer_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("war_6")));

    // ===== 倒吊人途径（秘祈人 / 倾听者 / 隐修士 / 蔷薇主教）=====

    /** 序列9 - 秘祈人魔药。 */
    public static final DeferredItem<SeerPotionItem> MYSTIC_PRAYER_POTION =
            ITEMS.register("mystic_prayer_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hanged_man_9")));

    /** 序列8 - 倾听者魔药。 */
    public static final DeferredItem<SeerPotionItem> LISTENER_POTION =
            ITEMS.register("listener_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hanged_man_8")));

    /** 序列7 - 隐修士魔药。 */
    public static final DeferredItem<SeerPotionItem> HERMIT_POTION =
            ITEMS.register("hermit_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hanged_man_7")));

    /** 序列6 - 蔷薇主教魔药。 */
    public static final DeferredItem<SeerPotionItem> ROSE_BISHOP_POTION =
            ITEMS.register("rose_bishop_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hanged_man_6")));

    // ===== 空想家途径（观众 / 读心者 / 心理医生 / 催眠师）=====

    /** 序列9 - 观众魔药。 */
    public static final DeferredItem<SeerPotionItem> SPECTATOR_POTION =
            ITEMS.register("spectator_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("visionary_9")));

    /** 序列8 - 读心者魔药。 */
    public static final DeferredItem<SeerPotionItem> MIND_READER_POTION =
            ITEMS.register("mind_reader_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("visionary_8")));

    /** 序列7 - 心理医生魔药。 */
    public static final DeferredItem<SeerPotionItem> PSYCHOLOGIST_POTION =
            ITEMS.register("psychologist_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("visionary_7")));

    /** 序列6 - 催眠师魔药。 */
    public static final DeferredItem<SeerPotionItem> HYPNOTIST_POTION =
            ITEMS.register("hypnotist_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("visionary_6")));

    // ===== 暴君途径（水手 / 暴怒之民 / 航海家 / 风眷者）=====

    /** 序列9 - 水手魔药。 */
    public static final DeferredItem<SeerPotionItem> SAILOR_POTION =
            ITEMS.register("sailor_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("tyrant_9")));

    /** 序列8 - 暴怒之民魔药。 */
    public static final DeferredItem<SeerPotionItem> WRATHFUL_POTION =
            ITEMS.register("wrathful_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("tyrant_8")));

    /** 序列7 - 航海家魔药。 */
    public static final DeferredItem<SeerPotionItem> NAVIGATOR_POTION =
            ITEMS.register("navigator_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("tyrant_7")));

    /** 序列6 - 风眷者魔药。 */
    public static final DeferredItem<SeerPotionItem> WIND_FAVORED_POTION =
            ITEMS.register("wind_favored_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("tyrant_6")));

    // ===== 太阳途径（歌颂者 / 祈光人 / 太阳神官 / 公证人）=====

    /** 序列9 - 歌颂者魔药。 */
    public static final DeferredItem<SeerPotionItem> PRAISER_POTION =
            ITEMS.register("praiser_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("sun_9")));

    /** 序列8 - 祈光人魔药。 */
    public static final DeferredItem<SeerPotionItem> LIGHT_SEEKER_POTION =
            ITEMS.register("light_seeker_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("sun_8")));

    /** 序列7 - 太阳神官魔药。 */
    public static final DeferredItem<SeerPotionItem> SUN_PRIEST_POTION =
            ITEMS.register("sun_priest_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("sun_7")));

    /** 序列6 - 公证人魔药。 */
    public static final DeferredItem<SeerPotionItem> NOTARY_POTION =
            ITEMS.register("notary_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("sun_6")));

    // ===== 白塔途径（阅读者 / 推理学员 / 守知者 / 博学者）=====

    /** 序列9 - 阅读者魔药。 */
    public static final DeferredItem<SeerPotionItem> READER_POTION =
            ITEMS.register("reader_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("white_tower_9")));

    /** 序列8 - 推理学员魔药。 */
    public static final DeferredItem<SeerPotionItem> REASONING_STUDENT_POTION =
            ITEMS.register("reasoning_student_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("white_tower_8")));

    /** 序列7 - 守知者魔药。 */
    public static final DeferredItem<SeerPotionItem> KNOWLEDGE_GUARDIAN_POTION =
            ITEMS.register("knowledge_guardian_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("white_tower_7")));

    /** 序列6 - 博学者魔药。 */
    public static final DeferredItem<SeerPotionItem> ERUDITE_POTION =
            ITEMS.register("erudite_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("white_tower_6")));

    // ===== 黄昏巨人途径（战士 / 格斗家 / 武器大师 / 黎明骑士）=====

    /** 序列9 - 战士魔药。 */
    public static final DeferredItem<SeerPotionItem> WARRIOR_POTION =
            ITEMS.register("warrior_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("giant_9")));

    /** 序列8 - 格斗家魔药。 */
    public static final DeferredItem<SeerPotionItem> FIGHTER_POTION =
            ITEMS.register("fighter_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("giant_8")));

    /** 序列7 - 武器大师魔药。 */
    public static final DeferredItem<SeerPotionItem> WEAPON_MASTER_POTION =
            ITEMS.register("weapon_master_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("giant_7")));

    /** 序列6 - 黎明骑士魔药。 */
    public static final DeferredItem<SeerPotionItem> DAWN_KNIGHT_POTION =
            ITEMS.register("dawn_knight_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("giant_6")));

    // ===== 黑暗途径（不眠者 / 午夜诗人 / 梦魇 / 安魂师）=====

    /** 序列9 - 不眠者魔药。 */
    public static final DeferredItem<SeerPotionItem> SLEEPLESS_POTION =
            ITEMS.register("sleepless_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("darkness_9")));

    /** 序列8 - 午夜诗人魔药。 */
    public static final DeferredItem<SeerPotionItem> MIDNIGHT_POET_POTION =
            ITEMS.register("midnight_poet_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("darkness_8")));

    /** 序列7 - 梦魇魔药。 */
    public static final DeferredItem<SeerPotionItem> NIGHTMARE_POTION =
            ITEMS.register("nightmare_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("darkness_7")));

    /** 序列6 - 安魂师魔药。 */
    public static final DeferredItem<SeerPotionItem> REQUIEM_POTION =
            ITEMS.register("requiem_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("darkness_6")));

    // ===== 死神途径（收尸人 / 掘墓人 / 通灵者 / 死灵导师）=====

    /** 序列9 - 收尸人魔药。 */
    public static final DeferredItem<SeerPotionItem> CORPSE_COLLECTOR_POTION =
            ITEMS.register("corpse_collector_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("death_9")));

    /** 序列8 - 掘墓人魔药。 */
    public static final DeferredItem<SeerPotionItem> GRAVEDIGGER_POTION =
            ITEMS.register("gravedigger_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("death_8")));

    /** 序列7 - 通灵者魔药。 */
    public static final DeferredItem<SeerPotionItem> SPIRIT_MEDIUM_POTION =
            ITEMS.register("spirit_medium_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("death_7")));

    /** 序列6 - 死灵导师魔药。 */
    public static final DeferredItem<SeerPotionItem> NECROMANCER_POTION =
            ITEMS.register("necromancer_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("death_6")));

    // ===== 黎明骑士（黄昏巨人途径）装备 =====

    /** 晨曦之剑：手持 3D 模型武器。 */
    public static final DeferredItem<DawnSwordItem> DAWN_SWORD =
            ITEMS.register("dawn_sword",
                    () -> new DawnSwordItem(new Item.Properties()));

    /** 岩浆之剑：深渊·序列6"恶魔"的标志性武器，手持 3D 模型。 */
    public static final DeferredItem<MagmaSwordItem> MAGMA_SWORD =
            ITEMS.register("magma_sword",
                    () -> new MagmaSwordItem(new Item.Properties()));

    /** 黎明铠甲：胸甲槽 3D 模型盔甲。 */
    public static final DeferredItem<DawnArmorItem> DAWN_ARMOR =
            ITEMS.register("dawn_armor",
                    () -> new DawnArmorItem(ModArmorMaterials.DAWN, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)));

    /** 狼人化变身（隐藏物品，仅作变身叠加模型的渲染载体）。 */
    public static final DeferredItem<WolfmanFormItem> WOLFMAN_FORM =
            ITEMS.register("wolfman_form",
                    () -> new WolfmanFormItem(ModArmorMaterials.DAWN, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)));

    /** 恶魔化变身（隐藏物品，仅作变身全身模型的渲染载体）。 */
    public static final DeferredItem<DemonFormItem> DEMON_FORM =
            ITEMS.register("demon_form",
                    () -> new DemonFormItem(ModArmorMaterials.DAWN, ArmorItem.Type.CHESTPLATE,
                            new Item.Properties().stacksTo(1)));

    // ===== 实验途径魔药（错误 / 门 / 完美者 / 隐者 / 月亮 / 母亲 / 深渊 / 被缚者 / 审判者 / 黑皇帝 / 命运之轮）=====

    // 错误途径
    public static final DeferredItem<SeerPotionItem> ERROR_9_POTION =
            ITEMS.register("error_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("error_9")));
    public static final DeferredItem<SeerPotionItem> ERROR_8_POTION =
            ITEMS.register("error_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("error_8")));
    public static final DeferredItem<SeerPotionItem> ERROR_7_POTION =
            ITEMS.register("error_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("error_7")));
    public static final DeferredItem<SeerPotionItem> ERROR_6_POTION =
            ITEMS.register("error_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("error_6")));

    // 门途径
    public static final DeferredItem<SeerPotionItem> DOOR_9_POTION =
            ITEMS.register("door_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("door_9")));
    public static final DeferredItem<SeerPotionItem> DOOR_8_POTION =
            ITEMS.register("door_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("door_8")));
    public static final DeferredItem<SeerPotionItem> DOOR_7_POTION =
            ITEMS.register("door_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("door_7")));
    public static final DeferredItem<SeerPotionItem> DOOR_6_POTION =
            ITEMS.register("door_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("door_6")));

    /** 空白符咒：门途径书写符咒的基础载体（素材 / 收藏品）。 */
    public static final DeferredItem<BlankTalismanItem> BLANK_TALISMAN =
            ITEMS.register("blank_talisman",
                    () -> new BlankTalismanItem(new Item.Properties().stacksTo(16)));

    // ===== 灵性符咒（祭台在灵性之墙内合成，投掷激发）=====

    /** 净化符咒：祈求太阳，重创鬼魂类怪物。 */
    public static final DeferredItem<com.wan.gmmod.common.item.TalismanItem> PURIFICATION_TALISMAN =
            ITEMS.register("purification_talisman",
                    () -> new com.wan.gmmod.common.item.TalismanItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.TALISMAN.get(), new TalismanData("sun", "purification"))));

    /** 安魂符咒：祈求黑夜，安抚鬼魂 / 幽影 / 僵尸 / 水鬼。 */
    public static final DeferredItem<com.wan.gmmod.common.item.TalismanItem> REQUIEM_TALISMAN =
            ITEMS.register("requiem_talisman",
                    () -> new com.wan.gmmod.common.item.TalismanItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.TALISMAN.get(), new TalismanData("night", "requiem"))));

    /** 电击符咒：祈求暴君，引发闪电与电击。 */
    public static final DeferredItem<com.wan.gmmod.common.item.TalismanItem> ELECTRIC_TALISMAN =
            ITEMS.register("electric_talisman",
                    () -> new com.wan.gmmod.common.item.TalismanItem(new Item.Properties().stacksTo(1)
                            .component(ModDataComponents.TALISMAN.get(), new TalismanData("tyrant", "electric"))));

    // ===== 货币（便士 / 苏勒 / 金镑，价值以便士计）=====

    /** 便士：最基础的货币，价值 1。 */
    public static final DeferredItem<com.wan.gmmod.common.item.CurrencyItem> PENNY =
            ITEMS.register("penny",
                    () -> new com.wan.gmmod.common.item.CurrencyItem(
                            new Item.Properties().stacksTo(64)
                                    .component(ModDataComponents.CURRENCY_VALUE.get(), 1)));

    /** 苏勒：价值 12 便士，12 便士可兑换。 */
    public static final DeferredItem<com.wan.gmmod.common.item.CurrencyItem> SOYLE =
            ITEMS.register("soyle",
                    () -> new com.wan.gmmod.common.item.CurrencyItem(
                            new Item.Properties().stacksTo(64)
                                    .component(ModDataComponents.CURRENCY_VALUE.get(), 12)));

    /** 金镑：价值 240 便士（= 20 苏勒），苏勒可兑换。 */
    public static final DeferredItem<com.wan.gmmod.common.item.CurrencyItem> GOLD_POUND =
            ITEMS.register("gold_pound",
                    () -> new com.wan.gmmod.common.item.CurrencyItem(
                            new Item.Properties().stacksTo(64)
                                    .component(ModDataComponents.CURRENCY_VALUE.get(), 240)));

    // 完美者途径
    public static final DeferredItem<SeerPotionItem> PARAGON_9_POTION =
            ITEMS.register("paragon_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("paragon_9")));
    public static final DeferredItem<SeerPotionItem> PARAGON_8_POTION =
            ITEMS.register("paragon_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("paragon_8")));
    public static final DeferredItem<SeerPotionItem> PARAGON_7_POTION =
            ITEMS.register("paragon_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("paragon_7")));
    public static final DeferredItem<SeerPotionItem> PARAGON_6_POTION =
            ITEMS.register("paragon_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("paragon_6")));

    // 隐者途径
    public static final DeferredItem<SeerPotionItem> HERMIT_9_POTION =
            ITEMS.register("hermit_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hermit_9")));
    public static final DeferredItem<SeerPotionItem> HERMIT_8_POTION =
            ITEMS.register("hermit_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hermit_8")));
    public static final DeferredItem<SeerPotionItem> HERMIT_7_POTION =
            ITEMS.register("hermit_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hermit_7")));
    public static final DeferredItem<SeerPotionItem> HERMIT_6_POTION =
            ITEMS.register("hermit_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("hermit_6")));

    // 月亮途径
    public static final DeferredItem<SeerPotionItem> MOON_9_POTION =
            ITEMS.register("moon_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("moon_9")));
    public static final DeferredItem<SeerPotionItem> MOON_8_POTION =
            ITEMS.register("moon_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("moon_8")));
    public static final DeferredItem<SeerPotionItem> MOON_7_POTION =
            ITEMS.register("moon_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("moon_7")));
    public static final DeferredItem<SeerPotionItem> MOON_6_POTION =
            ITEMS.register("moon_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("moon_6")));

    // 母亲途径
    public static final DeferredItem<SeerPotionItem> MOTHER_9_POTION =
            ITEMS.register("mother_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("mother_9")));
    public static final DeferredItem<SeerPotionItem> MOTHER_8_POTION =
            ITEMS.register("mother_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("mother_8")));
    public static final DeferredItem<SeerPotionItem> MOTHER_7_POTION =
            ITEMS.register("mother_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("mother_7")));
    public static final DeferredItem<SeerPotionItem> MOTHER_6_POTION =
            ITEMS.register("mother_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("mother_6")));

    // 深渊途径
    public static final DeferredItem<SeerPotionItem> ABYSS_9_POTION =
            ITEMS.register("abyss_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("abyss_9")));
    public static final DeferredItem<SeerPotionItem> ABYSS_8_POTION =
            ITEMS.register("abyss_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("abyss_8")));
    public static final DeferredItem<SeerPotionItem> ABYSS_7_POTION =
            ITEMS.register("abyss_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("abyss_7")));
    public static final DeferredItem<SeerPotionItem> ABYSS_6_POTION =
            ITEMS.register("abyss_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("abyss_6")));

    // 被缚者途径
    public static final DeferredItem<SeerPotionItem> CHAINED_9_POTION =
            ITEMS.register("chained_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("chained_9")));
    public static final DeferredItem<SeerPotionItem> CHAINED_8_POTION =
            ITEMS.register("chained_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("chained_8")));
    public static final DeferredItem<SeerPotionItem> CHAINED_7_POTION =
            ITEMS.register("chained_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("chained_7")));
    public static final DeferredItem<SeerPotionItem> CHAINED_6_POTION =
            ITEMS.register("chained_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("chained_6")));

    // 审判者途径
    public static final DeferredItem<SeerPotionItem> JUSTICE_9_POTION =
            ITEMS.register("justice_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("justice_9")));
    public static final DeferredItem<SeerPotionItem> JUSTICE_8_POTION =
            ITEMS.register("justice_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("justice_8")));
    public static final DeferredItem<SeerPotionItem> JUSTICE_7_POTION =
            ITEMS.register("justice_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("justice_7")));
    public static final DeferredItem<SeerPotionItem> JUSTICE_6_POTION =
            ITEMS.register("justice_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("justice_6")));

    // 黑皇帝途径
    public static final DeferredItem<SeerPotionItem> BLACK_EMPEROR_9_POTION =
            ITEMS.register("black_emperor_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("black_emperor_9")));
    public static final DeferredItem<SeerPotionItem> BLACK_EMPEROR_8_POTION =
            ITEMS.register("black_emperor_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("black_emperor_8")));
    public static final DeferredItem<SeerPotionItem> BLACK_EMPEROR_7_POTION =
            ITEMS.register("black_emperor_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("black_emperor_7")));
    public static final DeferredItem<SeerPotionItem> BLACK_EMPEROR_6_POTION =
            ITEMS.register("black_emperor_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("black_emperor_6")));

    // 命运之轮途径
    public static final DeferredItem<SeerPotionItem> WHEEL_9_POTION =
            ITEMS.register("wheel_9_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("wheel_9")));
    public static final DeferredItem<SeerPotionItem> WHEEL_8_POTION =
            ITEMS.register("wheel_8_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("wheel_8")));
    public static final DeferredItem<SeerPotionItem> WHEEL_7_POTION =
            ITEMS.register("wheel_7_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("wheel_7")));
    public static final DeferredItem<SeerPotionItem> WHEEL_6_POTION =
            ITEMS.register("wheel_6_potion",
                    () -> new SeerPotionItem(new Item.Properties(), GuimiMod.id("wheel_6")));

    
    // ===== 新增方块物品 =====
    public static final DeferredItem<BlockItem> LUCKY_FLOWER_SOIL =
            ITEMS.register("lucky_flower_soil",
                    () -> new BlockItem(ModBlocks.LUCKY_FLOWER_SOIL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SCORCHED_WOOD =
            ITEMS.register("scorched_wood",
                    () -> new BlockItem(ModBlocks.SCORCHED_WOOD.get(), new Item.Properties()));

    // ===== 好运圃植物方块物品 =====
    public static final DeferredItem<BlockItem> LUCKY_FLOWER =
            ITEMS.register("lucky_flower",
                    () -> new BlockItem(ModBlocks.LUCKY_FLOWER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FOUR_LEAF_CLOVER =
            ITEMS.register("four_leaf_clover",
                    () -> new BlockItem(ModBlocks.FOUR_LEAF_CLOVER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SILVER_FOUR_LEAF_CLOVER =
            ITEMS.register("silver_four_leaf_clover",
                    () -> new BlockItem(ModBlocks.SILVER_FOUR_LEAF_CLOVER.get(), new Item.Properties()));


    // ===== 96个材料物品（按pinyin名注册）=====

    public static final DeferredItem<Item> SHI_LING_ZHE_DE_WEI_DAI = ITEMS.register("shi_ling_zhe_de_wei_dai",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_MENG_HEI_YA_DE_HUAN_YU = ITEMS.register("shi_meng_hei_ya_de_huan_yu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_MENG_HEI_YA_DE_XIN_ZANG = ITEMS.register("shi_meng_hei_ya_de_xin_zang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_REN_TU_JIU_DE_HUI = ITEMS.register("shi_ren_tu_jiu_de_hui",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_REN_TU_JIU_DE_XUE_YE = ITEMS.register("shi_ren_tu_jiu_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_REN_ZHI_QUAN_DE_SHE_TOU = ITEMS.register("shi_ren_zhi_quan_de_she_tou",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_REN_ZHI_QUAN_DE_TUO_YE = ITEMS.register("shi_ren_zhi_quan_de_tuo_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_SHEN_REN_DE_DA_NAO = ITEMS.register("shi_shen_ren_de_da_nao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHI_XUE_XI_NIU_DE_JIAO = ITEMS.register("shi_xue_xi_niu_de_jiao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHOU_LIE_HEI_ZHU_DE_DU_XIAN = ITEMS.register("shou_lie_hei_zhu_de_du_xian",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHOU_LIE_HEI_ZHU_DE_FU_YAN = ITEMS.register("shou_lie_hei_zhu_de_fu_yan",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHU_CE = ITEMS.register("shu_ce",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHU_JI = ITEMS.register("shu_ji",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHU_QUN_ZHI_ZHU_DE_SHI_HAI = ITEMS.register("shu_qun_zhi_zhu_de_shi_hai",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHU_QUN_ZHI_ZHU_DE_XUE_YE = ITEMS.register("shu_qun_zhi_zhu_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHU_REN_JI_SI_DE_SHU_XIN = ITEMS.register("shu_ren_ji_si_de_shu_xin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHU_REN_JI_SI_DE_ZHI_YE = ITEMS.register("shu_ren_ji_si_de_zhi_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHUI_JING = ITEMS.register("shui_jing",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHUI_JUE_CAO = ITEMS.register("shui_jue_cao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHUI_JUE_ZHI_YE = ITEMS.register("shui_jue_zhi_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SHUI_XIAN_HUA_ZHI_YE = ITEMS.register("shui_xian_hua_zhi_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SI_BI_FENG_REN_DE_DA_NAO = ITEMS.register("si_bi_feng_ren_de_da_nao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SI_BI_FENG_REN_DE_XUE_YE = ITEMS.register("si_bi_feng_ren_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SI_WANG_ZHI_YING = ITEMS.register("si_wang_zhi_ying",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SI_WANG_ZHI_YING_DE_JI_JU_ZHI_WU = ITEMS.register("si_wang_zhi_ying_de_ji_ju_zhi_wu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SI_YU_E_LING_FU_SHEN_ZHI_REN_DE_NAO_JIANG = ITEMS.register("si_yu_e_ling_fu_shen_zhi_ren_de_nao_jiang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TA_REN_XUE_YE = ITEMS.register("ta_ren_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TA_REN_ZHI_XUE = ITEMS.register("ta_ren_zhi_xue",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TAI_YANG_HUA = ITEMS.register("tai_yang_hua",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TIE_BI_SHOU_REN_DE_JI_ZHUI = ITEMS.register("tie_bi_shou_ren_de_ji_zhui",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TIE_BI_SHOU_REN_DE_XUE_YE = ITEMS.register("tie_bi_shou_ren_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TU_PI = ITEMS.register("tu_pi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WAN_ZHENG_DE_A_SI_MAN_ZHI_NAO = ITEMS.register("wan_zheng_de_a_si_man_zhi_nao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WEI_REN_DE_TOU_FA = ITEMS.register("wei_ren_de_tou_fa",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WEI_REN_DE_XUE_YE_ZHI_YUAN = ITEMS.register("wei_ren_de_xue_ye_zhi_yuan",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WU_PI_XUE_MAO_DE_XIN_ZANG = ITEMS.register("wu_pi_xue_mao_de_xin_zang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WU_PI_XUE_MAO_DE_XUE_YE = ITEMS.register("wu_pi_xue_mao_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XIANG_FENG_CAO = ITEMS.register("xiang_feng_cao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XIE_DU_GUO_MOU_JIAN_JIAO_TANG_DE_WU_PIN = ITEMS.register("xie_du_guo_mou_jian_jiao_tang_de_wu_pin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XIN_GOU_XIANG_DE_QIE_MEI_YOU_ZHU_YAO_CUO_WU_DE_JI_XIE_SHE_JI_TU = ITEMS.register("xin_gou_xiang_de_qie_mei_you_zhu_yao_cuo_wu_de_ji_xie_she_ji_tu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XING_YUN_FU = ITEMS.register("xing_yun_fu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XIONG_MAN_ZHI_QUAN_DE_NAO_DAI = ITEMS.register("xiong_man_zhi_quan_de_nao_dai",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XIONG_MAN_ZHI_QUAN_DE_XUE_YE = ITEMS.register("xiong_man_zhi_quan_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XUE_BAN_HEI_WEN = ITEMS.register("xue_ban_hei_wen",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XUE_MEI_GUI_DE_YE_ZI = ITEMS.register("xue_mei_gui_de_ye_zi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XUE_XING_HUA_SU = ITEMS.register("xue_xing_hua_su",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XUE_YAN_HEI_SHAN_YANG_DE_DU_JIAO = ITEMS.register("xue_yan_hei_shan_yang_de_du_jiao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XUE_YAN_HEI_SHAN_YANG_DE_WEI_BA = ITEMS.register("xue_yan_hei_shan_yang_de_wei_ba",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> XUE_YUAN_JING_HUA = ITEMS.register("xue_yuan_jing_hua",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YAN_JIANG = ITEMS.register("yan_jiang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YAN_JIANG_JING_LING_DE_HE_XIN = ITEMS.register("yan_jiang_jing_ling_de_he_xin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YAN_JIANG_ZHI_MO_DE_DU_JIAO = ITEMS.register("yan_jiang_zhi_mo_de_du_jiao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YAN_JIANG_ZHI_MO_DE_RONG_RONG_ZHI_YE = ITEMS.register("yan_jiang_zhi_mo_de_rong_rong_zhi_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YANG_JIAO_MO_YU_DE_XUE_YE = ITEMS.register("yang_jiao_mo_yu_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YANG_XU_CAO = ITEMS.register("yang_xu_cao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YAO_GAO = ITEMS.register("yao_gao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YE_JIA_TI_LIAN_DE_ZHI_YE = ITEMS.register("ye_jia_ti_lian_de_zhi_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YE_MEI_GUI = ITEMS.register("ye_mei_gui",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YI_JI_PAI_HUAI_ZHE_DE_JI_SUI = ITEMS.register("yi_ji_pai_huai_zhe_de_ji_sui",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YI_JI_PAI_HUAI_ZHE_DE_XUE_YE = ITEMS.register("yi_ji_pai_huai_zhe_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YI_WEN_JU_HU_DE_WEI_BA = ITEMS.register("yi_wen_ju_hu_de_wei_ba",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YI_WEN_JU_HU_DE_YA_CHI = ITEMS.register("yi_wen_ju_hu_de_ya_chi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YI_YU_SHEN_MIAN_ZHE_DE_TOU_GU = ITEMS.register("yi_yu_shen_mian_zhe_de_tou_gu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_BAI_ZHAN_XIONG_DE_XUE_YE = ITEMS.register("yin_bai_zhan_xiong_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_BAI_ZHAN_XIONG_DE_YOU_ZHANG = ITEMS.register("yin_bai_zhan_xiong_de_you_zhang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_SE_JU_XIONG_DE_YOU_ZHANG = ITEMS.register("yin_se_ju_xiong_de_you_zhang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_SE_SI_YE_CAO = ITEMS.register("yin_se_si_ye_cao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_YAN_GUAI_SHE_DE_XUE_YE = ITEMS.register("yin_yan_guai_she_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_YAN_GUAI_SHE_DE_YAN_ZHU = ITEMS.register("yin_yan_guai_she_de_yan_zhu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_YING_DU_HUA = ITEMS.register("yin_ying_du_hua",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_YING_DU_HUA_DE_HUA_BAN = ITEMS.register("yin_ying_du_hua_de_hua_ban",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_YING_RU_CHONG = ITEMS.register("yin_ying_ru_chong",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YIN_YING_XI_YI_DE_LIN_PIAN = ITEMS.register("yin_ying_xi_yi_de_lin_pian",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YOU_LING_GUAI_MAO_DE_QIAN_ZHAO = ITEMS.register("you_ling_guai_mao_de_qian_zhao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YOU_LING_GUAI_MAO_DE_XUE_YE = ITEMS.register("you_ling_guai_mao_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YOU_NIAN_DU_JIAO_SHOU_DE_JIE_JING = ITEMS.register("you_nian_du_jiao_shou_de_jie_jing",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YOU_NIAN_DU_JIAO_SHOU_DE_XUE_YE = ITEMS.register("you_nian_du_jiao_shou_de_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YOU_SHAO_XU_SHEN_MI_LI_LIANG_DE_GU_DAI_WU_PIN = ITEMS.register("you_shao_xu_shen_mi_li_liang_de_gu_dai_wu_pin",
            () -> new Item(new Item.Properties()));

    // ===== 古代神秘物品（有少许神秘力量的古代物品系列实例）=====

    /** 破碎的圣像手指：来自血祭污染教堂的圣像断指，可充当「亵渎过教堂的物品」。 */
    public static final DeferredItem<com.wan.gmmod.content.ancient.AncientArtifactItem> BROKEN_ICON_FINGER =
            ITEMS.register("broken_icon_finger", () -> new com.wan.gmmod.content.ancient.AncientArtifactItem(
                    "broken_icon_finger", new Item.Properties()));

    /** 疯人院入院记录：记载他人秘密的纸。阅读窥得能力线索，污染 +10，可能引来失控者。 */
    public static final DeferredItem<com.wan.gmmod.content.ancient.AncientArtifactItem> ASYLUM_RECORD =
            ITEMS.register("asylum_record", () -> new com.wan.gmmod.content.ancient.AncientArtifactItem(
                    "asylum_record", new Item.Properties().stacksTo(16)));

    /** 焦灼的圣袍边角：来自太阳信仰的物品。手持近战附加火焰伤害，5% 概率引燃自己。 */
    public static final DeferredItem<com.wan.gmmod.content.ancient.AncientArtifactItem> SCORCHED_ROBE_FRAGMENT =
            ITEMS.register("scorched_robe_fragment", () -> new com.wan.gmmod.content.ancient.AncientArtifactItem(
                    "scorched_robe_fragment", new Item.Properties()));

    /** 血染的六便士：谋杀现场的沾血硬币。掷币问运：正面幸运，背面灾祸。 */
    public static final DeferredItem<com.wan.gmmod.content.ancient.AncientArtifactItem> BLOODSTAINED_SIXPENCE =
            ITEMS.register("bloodstained_sixpence", () -> new com.wan.gmmod.content.ancient.AncientArtifactItem(
                    "bloodstained_sixpence", new Item.Properties().stacksTo(16)));

    // ===== 魔药配方卷轴（全部序列共用一张卷轴纹理，仅名称与 tooltip 区分）=====

    /** 全部魔药配方卷轴：注册名 = recipe_scroll_<魔药注册名>，键为魔药注册名。 */
    public static final java.util.Map<String, DeferredItem<com.wan.gmmod.content.brewing.RecipeScrollItem>> RECIPE_SCROLLS =
            new java.util.LinkedHashMap<>();

    /** 需要配方卷轴的全部魔药（89 种）。 */
    private static final String[] SCROLL_POTIONS = {
            "seer_potion", "clown_potion", "magician_potion", "faceless_potion", "marionettist_potion",
            "spectator_potion", "mind_reader_potion", "psychologist_potion", "hypnotist_potion",
            "praiser_potion", "light_seeker_potion", "sun_priest_potion", "notary_potion",
            "sailor_potion", "wrathful_potion", "navigator_potion", "wind_favored_potion",
            "reader_potion", "reasoning_student_potion", "knowledge_guardian_potion", "erudite_potion",
            "mystic_prayer_potion", "listener_potion", "hermit_potion", "rose_bishop_potion",
            "error_9_potion", "error_8_potion", "error_7_potion", "error_6_potion",
            "door_9_potion", "door_8_potion", "door_7_potion", "door_6_potion",
            "hunter_potion", "provoker_potion", "pyromaniac_potion", "conspirer_potion",
            "assassin_potion", "instigator_potion", "witch_potion", "joyful_witch_potion",
            "black_emperor_9_potion", "black_emperor_8_potion", "black_emperor_7_potion", "black_emperor_6_potion",
            "justice_9_potion", "justice_8_potion", "justice_7_potion", "justice_6_potion",
            "sleepless_potion", "midnight_poet_potion", "nightmare_potion", "requiem_potion",
            "corpse_collector_potion", "gravedigger_potion", "spirit_medium_potion", "necromancer_potion",
            "warrior_potion", "fighter_potion", "weapon_master_potion", "dawn_knight_potion",
            "wheel_9_potion", "wheel_8_potion", "wheel_7_potion", "wheel_6_potion",
            "hermit_9_potion", "hermit_8_potion", "hermit_7_potion", "hermit_6_potion",
            "paragon_9_potion", "paragon_8_potion", "paragon_7_potion", "paragon_6_potion",
            "mother_9_potion", "mother_8_potion", "mother_7_potion", "mother_6_potion",
            "moon_9_potion", "moon_8_potion", "moon_7_potion", "moon_6_potion",
            "chained_9_potion", "chained_8_potion", "chained_7_potion", "chained_6_potion",
            "abyss_9_potion", "abyss_8_potion", "abyss_7_potion", "abyss_6_potion",
    };

    static {
        for (String potion : SCROLL_POTIONS) {
            RECIPE_SCROLLS.put(potion, ITEMS.register("recipe_scroll_" + potion,
                    () -> new com.wan.gmmod.content.brewing.RecipeScrollItem(potion, new Item.Properties().stacksTo(16))));
        }
    }

    // ===== 配方文档补充材料 =====

    public static final DeferredItem<Item> REN_MIAN_LONG_CAO = ITEMS.register("ren_mian_long_cao",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("ren_mian_long_cao_plant").get(), new Item.Properties()));
    public static final DeferredItem<Item> MU_YUAN_XUE_MEI_GUI = ITEMS.register("mu_yuan_xue_mei_gui",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("mu_yuan_xue_mei_gui_plant").get(), new Item.Properties()));
    public static final DeferredItem<Item> NA_JIA_JI_SI_DE_TOU_FA = ITEMS.register("na_jia_ji_si_de_tou_fa",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_DONG_HUA = ITEMS.register("ren_dong_hua",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("ren_dong_hua_plant").get(), new Item.Properties()));
    public static final DeferredItem<Item> SHE_HUN_FENG_LING_HUA = ITEMS.register("she_hun_feng_ling_hua",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHEN_HAI_NA_JIA_DE_TOU_PI = ITEMS.register("shen_hai_na_jia_de_tou_pi",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHEN_QIAN_ZHE_DE_SHE_TOU = ITEMS.register("shen_qian_zhe_de_she_tou",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHEN_MIAN_HUA = ITEMS.register("shen_mian_hua",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHI_SHEN_REN_DE_XUE_YE = ITEMS.register("shi_shen_ren_de_xue_ye",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> JIN_BIAN_TAI_YANG_HUA = ITEMS.register("jin_bian_tai_yang_hua",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("jin_bian_tai_yang_hua_plant").get(), new Item.Properties()));
    public static final DeferredItem<Item> GAO_SHAN_XUE_REN_DE_XUE_YE = ITEMS.register("gao_shan_xue_ren_de_xue_ye",
            () -> new Item(new Item.Properties()));

    // ===== 配方材料补全（纹理已就位）=====

    public static final DeferredItem<Item> E_YUN_HEI_MAO_DE_ER_DUO = ITEMS.register("e_yun_hei_mao_de_er_duo",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> E_YUN_HEI_MAO_DE_XIN_ZANG = ITEMS.register("e_yun_hei_mao_de_xin_zang",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIU_BI_NA_JIA_DE_GAN_ZANG = ITEMS.register("liu_bi_na_jia_de_gan_zang",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIU_BI_NA_JIA_DE_XUE_YE = ITEMS.register("liu_bi_na_jia_de_xue_ye",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_SHEN_SHI_DE_DA_NAO = ITEMS.register("ren_shen_shi_de_da_nao",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_SHEN_SHI_DE_XUE_YE = ITEMS.register("ren_shen_shi_de_xue_ye",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_LIAN_FEI_FEI_DE_XUE_YE = ITEMS.register("ren_lian_fei_fei_de_xue_ye",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_LEI_DE_NEI_ZANG = ITEMS.register("ren_lei_de_nei_zang",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FENG_SHOU_JU_REN_XUE_ZHE_DE_DA_NAO = ITEMS.register("feng_shou_ju_ren_xue_zhe_de_da_nao",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FENG_SHOU_JU_REN_XUE_ZHE_DE_SHE_JIAN = ITEMS.register("feng_shou_ju_ren_xue_zhe_de_she_jian",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DONG_WU_SHI_YOU = ITEMS.register("dong_wu_shi_you",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GUANG_HUI_QI_LING_SHU_DE_ZHI_YE = ITEMS.register("guang_hui_qi_ling_shu_de_zhi_ye",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_MING_WEN_SHU = ITEMS.register("ren_ming_wen_shu",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> QIN_SHOU_DA_MO_DE_CHI_LUN = ITEMS.register("qin_shou_da_mo_de_chi_lun",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REN_PI_GU_JUAN = ITEMS.register("ren_pi_gu_juan",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YU_REN_DE_BIAO = ITEMS.register("yu_ren_de_biao",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YU_REN_DE_LIN_PIAN = ITEMS.register("yu_ren_de_lin_pian",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YUE_CHANG_SHI = ITEMS.register("yue_chang_shi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> YUE_LIANG_HUA = ITEMS.register("yue_liang_hua",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHAN_HOU_NIAO_DE_HUI = ITEMS.register("zhan_hou_niao_de_hui",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHAN_HOU_NIAO_DE_XIONG_GU = ITEMS.register("zhan_hou_niao_de_xiong_gu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHANG_ZHE_ZHI_SHU_DE_GEN_JING_JIE_JING = ITEMS.register("zhang_zhe_zhi_shu_de_gen_jing_jie_jing",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHANG_ZHE_ZHI_SHU_DE_GUO_SHI = ITEMS.register("zhang_zhe_zhi_shu_de_guo_shi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHANG_ZHE_ZHI_SHU_DE_SHU_PI = ITEMS.register("zhang_zhe_zhi_shu_de_shu_pi",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHENG_LIU_JIU = ITEMS.register("zheng_liu_jiu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHI_LIAO_YAO_SHUI = ITEMS.register("zhi_liao_yao_shui",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHI_SHI_ZHI_SHU = ITEMS.register("zhi_shi_zhi_shu",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHI_YAO_DE_HUI_JIN = ITEMS.register("zhi_yao_de_hui_jin",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHI_YAO_DE_XU_HUAN_ZHI_YAN = ITEMS.register("zhi_yao_de_xu_huan_zhi_yan",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZHONG_XIA_CAO = ITEMS.register("zhong_xia_cao",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("zhong_xia_cao_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> ZI_SHEN_MOU_HAI_DE_DI_YI_GE_TONG_ZHONG_ZU_ZHE_DE_XIN_ZANG = ITEMS.register("zi_shen_mou_hai_de_di_yi_ge_tong_zhong_zu_zhe_de_xin_zang",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZI_SHEN_XUE_YE = ITEMS.register("zi_shen_xue_ye",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZI_SHEN_ZHI_ZAO_DE_YAO_JI = ITEMS.register("zi_shen_zhi_zao_de_yao_ji",
            () -> new Item(new Item.Properties()));

// ===== 材料物品（277个，自动生成）=====

    public static final DeferredItem<Item> MAT_001 = ITEMS.register("material_001",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_002 = ITEMS.register("material_002",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_003 = ITEMS.register("material_003",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_004 = ITEMS.register("material_004",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_005 = ITEMS.register("material_005",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_006 = ITEMS.register("material_006",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_007 = ITEMS.register("material_007",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_008 = ITEMS.register("material_008",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_009 = ITEMS.register("material_009",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_010 = ITEMS.register("material_010",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_011 = ITEMS.register("material_011",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_012 = ITEMS.register("material_012",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_013 = ITEMS.register("material_013",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_014 = ITEMS.register("material_014",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_015 = ITEMS.register("material_015",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_016 = ITEMS.register("material_016",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_017 = ITEMS.register("material_017",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_018 = ITEMS.register("material_018",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_019 = ITEMS.register("material_019",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_020 = ITEMS.register("material_020",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_021 = ITEMS.register("material_021",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_022 = ITEMS.register("material_022",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_023 = ITEMS.register("material_023",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_024 = ITEMS.register("material_024",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_025 = ITEMS.register("material_025",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_026 = ITEMS.register("material_026",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_027 = ITEMS.register("material_027",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_028 = ITEMS.register("material_028",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_029 = ITEMS.register("material_029",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_030 = ITEMS.register("material_030",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_031 = ITEMS.register("material_031",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_032 = ITEMS.register("material_032",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_033 = ITEMS.register("material_033",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_034 = ITEMS.register("material_034",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_035 = ITEMS.register("material_035",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_036 = ITEMS.register("material_036",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_037 = ITEMS.register("material_037",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_038 = ITEMS.register("material_038",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_039 = ITEMS.register("material_039",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_040 = ITEMS.register("material_040",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_041 = ITEMS.register("material_041",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_042 = ITEMS.register("material_042",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_043 = ITEMS.register("material_043",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_044 = ITEMS.register("material_044",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_045 = ITEMS.register("material_045",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_046 = ITEMS.register("material_046",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_047 = ITEMS.register("material_047",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_048 = ITEMS.register("material_048",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_049 = ITEMS.register("material_049",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_050 = ITEMS.register("material_050",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_051 = ITEMS.register("material_051",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_052 = ITEMS.register("material_052",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_053 = ITEMS.register("material_053",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_054 = ITEMS.register("material_054",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_055 = ITEMS.register("material_055",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_056 = ITEMS.register("material_056",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_057 = ITEMS.register("material_057",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_058 = ITEMS.register("material_058",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_059 = ITEMS.register("material_059",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_060 = ITEMS.register("material_060",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_061 = ITEMS.register("material_061",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_062 = ITEMS.register("material_062",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_063 = ITEMS.register("material_063",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_064 = ITEMS.register("material_064",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_065 = ITEMS.register("material_065",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_066 = ITEMS.register("material_066",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_067 = ITEMS.register("material_067",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_068 = ITEMS.register("material_068",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_069 = ITEMS.register("material_069",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_070 = ITEMS.register("material_070",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_071 = ITEMS.register("material_071",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_072 = ITEMS.register("material_072",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_073 = ITEMS.register("material_073",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_074 = ITEMS.register("material_074",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_075 = ITEMS.register("material_075",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_076 = ITEMS.register("material_076",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_077 = ITEMS.register("material_077",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_078 = ITEMS.register("material_078",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_079 = ITEMS.register("material_079",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_080 = ITEMS.register("material_080",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_081 = ITEMS.register("material_081",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_082 = ITEMS.register("material_082",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_083 = ITEMS.register("material_083",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_084 = ITEMS.register("material_084",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_084_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_085 = ITEMS.register("material_085",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_086 = ITEMS.register("material_086",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_087 = ITEMS.register("material_087",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_088 = ITEMS.register("material_088",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_089 = ITEMS.register("material_089",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_090 = ITEMS.register("material_090",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_091 = ITEMS.register("material_091",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_092 = ITEMS.register("material_092",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_092_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_093 = ITEMS.register("material_093",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_094 = ITEMS.register("material_094",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_095 = ITEMS.register("material_095",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_096 = ITEMS.register("material_096",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_097 = ITEMS.register("material_097",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_098 = ITEMS.register("material_098",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_099 = ITEMS.register("material_099",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_100 = ITEMS.register("material_100",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_101 = ITEMS.register("material_101",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_102 = ITEMS.register("material_102",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_103 = ITEMS.register("material_103",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_104 = ITEMS.register("material_104",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_105 = ITEMS.register("material_105",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_106 = ITEMS.register("material_106",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_107 = ITEMS.register("material_107",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_108 = ITEMS.register("material_108",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_109 = ITEMS.register("material_109",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_110 = ITEMS.register("material_110",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_111 = ITEMS.register("material_111",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_112 = ITEMS.register("material_112",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_113 = ITEMS.register("material_113",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_113_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_114 = ITEMS.register("material_114",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_115 = ITEMS.register("material_115",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_116 = ITEMS.register("material_116",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_117 = ITEMS.register("material_117",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_118 = ITEMS.register("material_118",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_119 = ITEMS.register("material_119",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_120 = ITEMS.register("material_120",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_121 = ITEMS.register("material_121",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_122 = ITEMS.register("material_122",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_123 = ITEMS.register("material_123",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_124 = ITEMS.register("material_124",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_125 = ITEMS.register("material_125",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_126 = ITEMS.register("material_126",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_127 = ITEMS.register("material_127",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_127_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_128 = ITEMS.register("material_128",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_128_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_129 = ITEMS.register("material_129",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_129_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_130 = ITEMS.register("material_130",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_130_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_131 = ITEMS.register("material_131",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_132 = ITEMS.register("material_132",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_133 = ITEMS.register("material_133",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_134 = ITEMS.register("material_134",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_135 = ITEMS.register("material_135",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_136 = ITEMS.register("material_136",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_137 = ITEMS.register("material_137",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_138 = ITEMS.register("material_138",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_139 = ITEMS.register("material_139",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_140 = ITEMS.register("material_140",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_141 = ITEMS.register("material_141",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_142 = ITEMS.register("material_142",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_143 = ITEMS.register("material_143",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_144 = ITEMS.register("material_144",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_144_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_145 = ITEMS.register("material_145",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_146 = ITEMS.register("material_146",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_147 = ITEMS.register("material_147",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_148 = ITEMS.register("material_148",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_148_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_149 = ITEMS.register("material_149",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_150 = ITEMS.register("material_150",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_151 = ITEMS.register("material_151",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_152 = ITEMS.register("material_152",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_153 = ITEMS.register("material_153",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_154 = ITEMS.register("material_154",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_155 = ITEMS.register("material_155",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_156 = ITEMS.register("material_156",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_156_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_157 = ITEMS.register("material_157",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_158 = ITEMS.register("material_158",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_159 = ITEMS.register("material_159",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_160 = ITEMS.register("material_160",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_161 = ITEMS.register("material_161",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_162 = ITEMS.register("material_162",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_163 = ITEMS.register("material_163",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_164 = ITEMS.register("material_164",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_165 = ITEMS.register("material_165",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_166 = ITEMS.register("material_166",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_167 = ITEMS.register("material_167",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_168 = ITEMS.register("material_168",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_169 = ITEMS.register("material_169",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_170 = ITEMS.register("material_170",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_171 = ITEMS.register("material_171",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_171_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_172 = ITEMS.register("material_172",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_173 = ITEMS.register("material_173",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_174 = ITEMS.register("material_174",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_175 = ITEMS.register("material_175",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_176 = ITEMS.register("material_176",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_177 = ITEMS.register("material_177",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_178 = ITEMS.register("material_178",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_179 = ITEMS.register("material_179",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_180 = ITEMS.register("material_180",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_181 = ITEMS.register("material_181",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_182 = ITEMS.register("material_182",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_183 = ITEMS.register("material_183",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_184 = ITEMS.register("material_184",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_184_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_185 = ITEMS.register("material_185",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_186 = ITEMS.register("material_186",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_187 = ITEMS.register("material_187",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_188 = ITEMS.register("material_188",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_189 = ITEMS.register("material_189",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_190 = ITEMS.register("material_190",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_191 = ITEMS.register("material_191",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_192 = ITEMS.register("material_192",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_193 = ITEMS.register("material_193",
            () -> new com.wan.gmmod.common.item.SyringeItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MAT_194 = ITEMS.register("material_194",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_195 = ITEMS.register("material_195",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_196 = ITEMS.register("material_196",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_197 = ITEMS.register("material_197",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_198 = ITEMS.register("material_198",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_199 = ITEMS.register("material_199",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_200 = ITEMS.register("material_200",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_201 = ITEMS.register("material_201",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_202 = ITEMS.register("material_202",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_203 = ITEMS.register("material_203",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_204 = ITEMS.register("material_204",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_205 = ITEMS.register("material_205",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_206 = ITEMS.register("material_206",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_207 = ITEMS.register("material_207",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_208 = ITEMS.register("material_208",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_209 = ITEMS.register("material_209",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_210 = ITEMS.register("material_210",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_211 = ITEMS.register("material_211",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_212 = ITEMS.register("material_212",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_213 = ITEMS.register("material_213",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_214 = ITEMS.register("material_214",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_215 = ITEMS.register("material_215",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_216 = ITEMS.register("material_216",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_217 = ITEMS.register("material_217",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_218 = ITEMS.register("material_218",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_219 = ITEMS.register("material_219",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_220 = ITEMS.register("material_220",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_221 = ITEMS.register("material_221",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_222 = ITEMS.register("material_222",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_223 = ITEMS.register("material_223",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_224 = ITEMS.register("material_224",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_225 = ITEMS.register("material_225",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_226 = ITEMS.register("material_226",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_226_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_227 = ITEMS.register("material_227",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_227_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_228 = ITEMS.register("material_228",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_229 = ITEMS.register("material_229",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_230 = ITEMS.register("material_230",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_231 = ITEMS.register("material_231",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_232 = ITEMS.register("material_232",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_233 = ITEMS.register("material_233",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_234 = ITEMS.register("material_234",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_235 = ITEMS.register("material_235",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_236 = ITEMS.register("material_236",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_237 = ITEMS.register("material_237",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_238 = ITEMS.register("material_238",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_239 = ITEMS.register("material_239",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_240 = ITEMS.register("material_240",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_241 = ITEMS.register("material_241",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_242 = ITEMS.register("material_242",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_243 = ITEMS.register("material_243",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_244 = ITEMS.register("material_244",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_245 = ITEMS.register("material_245",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_246 = ITEMS.register("material_246",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_247 = ITEMS.register("material_247",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_247_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_248 = ITEMS.register("material_248",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_249 = ITEMS.register("material_249",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_250 = ITEMS.register("material_250",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_251 = ITEMS.register("material_251",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_252 = ITEMS.register("material_252",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_253 = ITEMS.register("material_253",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_254 = ITEMS.register("material_254",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_255 = ITEMS.register("material_255",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_256 = ITEMS.register("material_256",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_256_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_257 = ITEMS.register("material_257",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_258 = ITEMS.register("material_258",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_259 = ITEMS.register("material_259",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_260 = ITEMS.register("material_260",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_261 = ITEMS.register("material_261",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_261_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_262 = ITEMS.register("material_262",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_263 = ITEMS.register("material_263",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_263_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_264 = ITEMS.register("material_264",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_265 = ITEMS.register("material_265",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_266 = ITEMS.register("material_266",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_267 = ITEMS.register("material_267",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_268 = ITEMS.register("material_268",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_269 = ITEMS.register("material_269",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_269_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_270 = ITEMS.register("material_270",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_271 = ITEMS.register("material_271",
            () -> new BlockItem(ModBlocks.GENERATED_PLANTS.get("material_271_plant").get(), new Item.Properties()));

    public static final DeferredItem<Item> MAT_272 = ITEMS.register("material_272",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_273 = ITEMS.register("material_273",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_274 = ITEMS.register("material_274",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_275 = ITEMS.register("material_275",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_276 = ITEMS.register("material_276",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAT_277 = ITEMS.register("material_277",
            () -> new Item(new Item.Properties()));
}
