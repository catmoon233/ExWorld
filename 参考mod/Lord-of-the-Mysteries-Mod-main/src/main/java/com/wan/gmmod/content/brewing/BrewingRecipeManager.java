package com.wan.gmmod.content.brewing;

import com.wan.gmmod.common.registry.ModBlocks;
import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 炼药锅配方注册表：集中存储所有魔药配方，供炼药事件在搅拌完成时匹配。
 * <p>
 * 通过 {@link #register(BrewingRecipe)} 追加配方，{@link #init()} 在通用初始化阶段
 * 注册内置配方（此时物品已完成注册）。配方按注册顺序匹配，返回首个满足的配方。
 * <p>
 * 数量换算约定（源自魔药配方.docx）：
 * <ul>
 *     <li>计数单位（朵/片/根/颗/个/只/对/块/份/册/张/枚/粒/株/截/滴）→ 保留数字；</li>
 *     <li>容量/重量（毫升/克）为风味描述 → 统一记 1 份；</li>
 *     <li>未标注数量 → 1 份。</li>
 * </ul>
 * 文档中暂无对应物品的材料已在配方注释中标明「缺」，未计入配方。
 */
public final class BrewingRecipeManager {

    private static final List<BrewingRecipe> RECIPES = new ArrayList<>();

    private BrewingRecipeManager() {
    }

    /** 追加一条配方。 */
    public static void register(BrewingRecipe recipe) {
        RECIPES.add(recipe);
    }

    /**
     * 依据锅内材料匹配配方，返回首个满足的配方；无匹配返回 {@code null}。
     *
     * @param available 锅内材料物品 → 现有数量
     */
    public static BrewingRecipe findMatch(Map<Item, Integer> available) {
        for (BrewingRecipe recipe : RECIPES) {
            if (recipe.matches(available)) {
                return recipe;
            }
        }
        return null;
    }

    /** 按配方 ID（产物魔药注册名 path）查询配方；无则返回 {@code null}。 */
    public static BrewingRecipe byId(String id) {
        for (BrewingRecipe recipe : RECIPES) {
            if (recipe.id().equals(id)) {
                return recipe;
            }
        }
        return null;
    }

    /** 注册内置配方。幂等：重复调用会先清空既有配方，避免重复注册。 */
    public static void init() {
        RECIPES.clear();

        // ===== 愚者途径 =====

        // 序列9·占卜家：拉瓦章鱼血液x1 + 星水晶x3 + 夜香草x5 + 金薄荷叶子x7 + 龙血草x3 + 毒堇x2
        add(ModItems.SEER_POTION,
                ModItems.LAVA_OCTOPUS_BLOOD, 1, ModItems.STAR_CRYSTAL, 3,
                ModBlocks.NIGHT_FRAGRANCE, 5,
                ModItems.GOLD_MINT_LEAF, 7,
                ModBlocks.DRAGON_BLOOD_GRASS, 3,
                ModBlocks.POISON_HEMLOCK, 2);

        // 序列8·小丑：霍纳奇斯羊角晶x1 + 人脸玫瑰x1 + 曼陀罗x5 + 黑边太阳花x5 + 金斗蓬草x1 + 毒堇x3
        add(ModItems.CLOWN_POTION,
                ModItems.HORNACIS_GOAT_HORN_CRYSTAL, 1, ModItems.FACE_ROSE, 1,
                ModItems.MANDRAGORA, 5, ModItems.BLACK_EDGED_SUNFLOWER, 5,
                ModItems.GOLDEN_CLOAK_GRASS, 1,
                ModBlocks.POISON_HEMLOCK, 3);

        // 序列7·魔术师：迷雾树人根茎x1 + 邪纹黑豹脊髓液x1 + 迷雾树人汁液x1 + 水形宝石x3 + 迷幻草x4
        add(ModItems.MAGICIAN_POTION,
                ModItems.MIST_TREANT_ROOT, 1, ModItems.EVIL_PANTHER_SPINAL_FLUID, 1,
                ModItems.MIST_TREANT_JUICE, 1, ModItems.WATER_SHAPE_GEM, 3,
                ModItems.PSYCHEDELIC_GRASS, 4);

        // 序列6·无面人：千面猎人血液x1 + 脑垂体x1 + 人皮幽影特性x1 + 黑色曼陀罗x3 + 龙牙草x2
        add(ModItems.FACELESS_POTION,
                ModItems.THOUSAND_FACED_HUNTER_BLOOD, 1, ModItems.THOUSAND_FACED_HUNTER_PITUITARY, 1,
                ModItems.HUMAN_SKIN_SHADOW_CHARACTERISTIC, 1, ModItems.BLACK_MANDRAGORA, 3,
                ModItems.AGRIMONY, 2);

        // 序列5·秘偶大师：古老怨灵粉尘x1 + 六翼石像鬼核心结晶x1 + 苏尼亚金泉泉水x1 + 龙纹树皮x1 + 残余灵性x1 + 石像鬼眼睛x1
        add(ModItems.MARIONETTIST_POTION,
                ModItems.ANCIENT_WRAITH_DUST, 1, ModItems.SIX_WINGED_GARGOYLE_CORE_CRYSTAL, 1,
                ModItems.SONIA_GOLDEN_SPRING_WATER, 1, ModItems.DRAGON_PATTERN_TREE_BARK, 1,
                ModItems.ANCIENT_WRAITH_RESIDUAL_SPIRITUALITY, 1, ModItems.SIX_WINGED_GARGOYLE_EYE, 1);

        // ===== 观众途径（空想家）=====

        // 序列9·观众：羊角墨鱼血液 + 成年曼哈尔鱼的眼睛 + 秋山仙华滴 + 牛赤芍药粉末x2 + 精灵花花瓣x7 + 纯水
        add(ModItems.SPECTATOR_POTION,
                ModItems.MAT_155, 1, ModItems.MAT_054, 1, ModItems.MAT_139, 1,
                ModItems.MAT_111, 2, ModItems.MAT_143, 7, ModItems.PURIFIED_WATER, 1);

        // 序列8·读心者：法尔斯曼兔脊髓液 + 七彩蜥龙脑垂体 + 橡树树苗 + 龙牙草x7 + 纯白精灵花花瓣 + 纯水
        add(ModItems.MIND_READER_POTION,
                ModItems.MAT_086, 1, ModItems.MAT_052, 1, Items.OAK_SAPLING, 1,
                ModItems.AGRIMONY, 7, ModItems.MAT_151, 1, ModItems.PURIFIED_WATER, 1);

        // 序列7·心理医生：镜龙的眼睛 + 长者之树的果实 + 镜龙的血液 + 长者之树的树皮x3 + 狐狸皮
        add(ModItems.PSYCHOLOGIST_POTION,
                ModItems.MAT_203, 1, ModItems.MAT_205, 1, ModItems.MAT_204, 1,
                ModItems.MAT_206, 3, ModItems.MAT_116, 1);

        // 序列6·催眠师：黑狩巨蜥脊髓液 + 迷幻风铃树果实 + 心灵巨龙脑垂体 + 心灵巨龙血液 + 鳞片x3 + 镜子
        add(ModItems.HYPNOTIST_POTION,
                ModItems.MAT_246, 1, ModItems.MAT_183, 1, ModItems.MAT_214, 1,
                ModItems.MAT_215, 1, ModItems.MAT_216, 3, ModItems.MIRROR, 1);

        // ===== 太阳途径 =====

        // 序列9·歌颂者：太阳花 + 海妖之石 + 仲夏草 + 精灵暗叶
        add(ModItems.PRAISER_POTION,
                ModItems.MAT_261, 1, ModItems.MAT_091, 1, ModItems.ZHONG_XIA_CAO, 1, ModItems.MAT_142, 1);

        // 序列8·祈光人：炽白之魂粉末 + 镜猬血液 + 金边太阳花 + 焦灼的圣袍边角（来自太阳信仰的物品）
        add(ModItems.LIGHT_SEEKER_POTION,
                ModItems.MAT_108, 1, ModItems.MAT_202, 1,
                ModItems.JIN_BIAN_TAI_YANG_HUA, 1, ModItems.SCORCHED_ROBE_FRAGMENT, 1);

        // 序列7·太阳神官：黎明雄鸡红冠 + 黎明雄鸡血液 + 太阳花x10（原文「太阳化x10」）+ 岩浆膏x5
        add(ModItems.SUN_PRIEST_POTION,
                ModItems.MAT_238, 1, ModItems.MAT_239, 1, ModItems.MAT_261, 10, Items.MAGMA_CREAM, 5);

        // 序列6·公证人：长者之树根茎结晶 + 契灵鸟尾羽x5 + 金边太阳花 + 白边太阳花 + 水蕨汁液x5 + 光辉契灵树的汁液
        add(ModItems.NOTARY_POTION,
                ModItems.MAT_207, 1, ModItems.MAT_021, 5, ModItems.JIN_BIAN_TAI_YANG_HUA, 1,
                ModItems.MAT_130, 1, ModItems.MAT_083, 5, ModItems.GUANG_HUI_QI_LING_SHU_DE_ZHI_YE, 1);

        // ===== 水手途径（暴君）=====

        // 序列9·水手：鱼人的鳔 + 鹦鹉螺壳 + 烈酒 + 纯水 + 鱼人鳞片x3 + 海带x12
        add(ModItems.SAILOR_POTION,
                ModItems.MAT_274, 1, Items.NAUTILUS_SHELL, 1, ModItems.MAT_110, 1,
                ModItems.PURIFIED_WATER, 1, ModItems.MAT_233, 3, Items.KELP, 12);

        // 序列8·暴怒之民：深潜者肺泡 + 战吼鸟胸骨 + 深潜者的舌头 + 战吼鸟的喙
        add(ModItems.WRATHFUL_POTION,
                ModItems.MAT_095, 1, ModItems.MAT_058, 1,
                ModItems.SHEN_QIAN_ZHE_DE_SHE_TOU, 1, ModItems.MAT_057, 1);

        // 序列7·航海家：黑鳞鲨脑垂体 + 狂暴白豚心脏 + 黑鳞鲨鳍翅 + 狂暴白豚血液 + 烈酒
        add(ModItems.NAVIGATOR_POTION,
                ModItems.MAT_250, 1, ModItems.MAT_114, 1, ModItems.MAT_251, 1,
                ModItems.MAT_115, 1, ModItems.MAT_110, 1);

        // 序列6·风眷者：蓝影隼结晶羽毛x6 + 龙眼海雕眼珠 + 蓝影隼骨头 + 龙眼海雕羽毛x2 + 烈酒
        add(ModItems.WIND_FAVORED_POTION,
                ModItems.MAT_167, 6, ModItems.MAT_254, 1, ModItems.MAT_168, 1,
                ModItems.MAT_255, 2, ModItems.MAT_110, 1);

        // ===== 阅读者途径（白塔）=====

        // 序列9·阅读者：知识之书 + 附魔书x2 + 墨囊 + 书与笔
        add(ModItems.READER_POTION,
                ModItems.MAT_137, 1, Items.ENCHANTED_BOOK, 2, Items.INK_SAC, 1, Items.WRITABLE_BOOK, 1);

        // 序列8·推理学员：娜迦祭司大脑 + 娜迦祭司的头发x9 + 黑色曼陀罗x5 + 水瓶
        add(ModItems.REASONING_STUDENT_POTION,
                ModItems.MAT_023, 1, ModItems.NA_JIA_JI_SI_DE_TOU_FA, 9,
                ModItems.BLACK_MANDRAGORA, 5, Items.GLASS_BOTTLE, 1);

        // 序列7·守知者（侦探）：六臂娜迦的肝脏 + 六臂娜迦的血液 + 知识之书 + 墨囊x10
        add(ModItems.KNOWLEDGE_GUARDIAN_POTION,
                ModItems.LIU_BI_NA_JIA_DE_GAN_ZANG, 1, ModItems.LIU_BI_NA_JIA_DE_XUE_YE, 1,
                ModItems.MAT_137, 1, Items.INK_SAC, 10);

        // 序列6·博学者：白尾赤狐胃袋 + 白尾赤狐血液 + 深海娜迦眼珠 + 深海娜迦的头皮 + 新知识（以知识卷轴替代）
        add(ModItems.ERUDITE_POTION,
                ModItems.MAT_125, 1, ModItems.MAT_126, 1, ModItems.MAT_093, 1,
                ModItems.SHEN_HAI_NA_JIA_DE_TOU_PI, 1, ModItems.MAT_138, 1);

        // ===== 秘祈人途径（倒吊人）=====

        // 序列9·秘祈人：蘑菇 + 深黯蝙蝠 + 红葡萄酒 + 蜡烛 + 任意蘑菇
        add(ModItems.MYSTIC_PRAYER_POTION,
                Items.RED_MUSHROOM, 1, ModItems.MAT_096, 1, ModItems.MAT_149, 1,
                Items.CANDLE, 1, Items.BROWN_MUSHROOM, 1);

        // 序列8·倾听者：厄运黑猫的耳朵 + 地穴蜘蛛的心脏 + 厄运黑猫的心脏 + 红葡萄酒 + 面包 + 海棠花
        add(ModItems.LISTENER_POTION,
                ModItems.E_YUN_HEI_MAO_DE_ER_DUO, 1, ModItems.MAT_017, 1,
                ModItems.E_YUN_HEI_MAO_DE_XIN_ZANG, 1, ModItems.MAT_149, 1, Items.BREAD, 1, ModItems.MAT_092, 1);

        // 序列7·隐修士：阴影蠕虫 + 血眼黑山羊独角 + 他人血液 + 血眼黑山羊尾巴 + 破碎的圣像手指（亵渎过教堂的物品）+ 玫瑰（以野玫瑰替代）
        add(ModItems.HERMIT_POTION,
                ModItems.MAT_213, 1, ModItems.MAT_177, 1, ModItems.TA_REN_XUE_YE, 1,
                ModItems.MAT_176, 1, ModItems.BROKEN_ICON_FINGER, 1, ModItems.MAT_271, 1);

        // 序列6·蔷薇主教：腐肉x128 + 复仇之影最大的碎片 + 自身血液 + 复仇之影残留粉末 + 肉桂
        add(ModItems.ROSE_BISHOP_POTION,
                Items.ROTTEN_FLESH, 128, ModItems.MAT_018, 1, ModItems.ZI_SHEN_XUE_YE, 1,
                ModItems.MAT_019, 1, ModItems.MAT_157, 1);

        // ===== 偷盗者途径（错误）=====

        // 序列9·偷盗者：血斑黑蚊 + 他人之血 + 蓝宝石 + 马鞭草
        add(ModItems.ERROR_9_POTION,
                ModItems.MAT_173, 1, ModItems.TA_REN_XUE_YE, 1, ModItems.MAT_164, 1, ModItems.MAT_227, 1);

        // 序列8·诈骗师：人面笼草 + 纯水 + 青金石 + 滨菊
        add(ModItems.ERROR_8_POTION,
                ModItems.REN_MIAN_LONG_CAO, 1, ModItems.PURIFIED_WATER, 1,
                Items.LAPIS_LAZULI, 1, Items.OXEYE_DAISY, 1);

        // 序列7·解密学者：狮身人大脑 + 狮身人的血液 + 月长石 + 野玫瑰
        add(ModItems.ERROR_7_POTION,
                ModItems.MAT_121, 1, ModItems.SHI_SHEN_REN_DE_XUE_YE, 1,
                ModItems.MAT_069, 1, ModItems.MAT_271, 1);

        // 序列6·盗火人：披袍幽魂附着物 + 美酒（以烈酒替代）+ 披袍幽魂残留粉末 + 黄水晶 + 树苗（以云杉树苗替代）+ 火把
        add(ModItems.ERROR_6_POTION,
                ModItems.MAT_179, 1, ModItems.MAT_110, 1, ModItems.MAT_059, 1,
                ModItems.MAT_237, 1, Items.SPRUCE_SAPLING, 1, Items.TORCH, 1);

        // ===== 学徒途径（门）=====

        // 序列9·学徒：幻想晶石 + 羊须草x10 + 纯水 + 从尸体长出的任意花朵
        add(ModItems.DOOR_9_POTION,
                ModItems.MAT_040, 1, ModItems.YANG_XU_CAO, 10, ModItems.PURIFIED_WATER, 1, ModItems.MAT_026, 1);

        // 序列8·戏法大师：食灵者胃袋 + 深海枪鱼血液 + 虞美人 + 纯水 + 火药
        add(ModItems.DOOR_8_POTION,
                ModItems.MAT_225, 1, ModItems.MAT_094, 1, Items.POPPY, 1,
                ModItems.PURIFIED_WATER, 1, Items.GUNPOWDER, 1);

        // 序列7·占星人：星水晶x5 + 拉瓦章鱼的血液（血液结晶以血液替代）+ 水晶x5 + 烈酒x8
        add(ModItems.DOOR_7_POTION,
                ModItems.STAR_CRYSTAL, 5, ModItems.LAVA_OCTOPUS_BLOOD, 1,
                ModItems.MAT_082, 5, ModItems.MAT_110, 8);

        // 序列6·记录官：完整的阿斯曼之脑 + 古老怨灵的诅咒物 + 知识之书x3 + 纯水x3
        add(ModItems.DOOR_6_POTION,
                ModItems.MAT_024, 1, ModItems.MAT_005, 1, ModItems.MAT_137, 3, ModItems.PURIFIED_WATER, 3);

        // ===== 红祭司途径（战争之红，按配方文档更新）=====

        // 序列9·猎人：血腥花粟 + 任意生物肉x20（以牛肉代表）+ 红葡萄酒 + 红粟花 + 白桦树树叶x5
        add(ModItems.HUNTER_POTION,
                ModItems.MAT_178, 1, Items.BEEF, 20, ModItems.MAT_149, 1,
                ModItems.MAT_148, 1, Items.BIRCH_LEAVES, 5);

        // 序列8·挑衅者：尖齿鹦鹉的舌头 + 蒸馏酒 + 忍冬花 + 水蕨草
        add(ModItems.PROVOKER_POTION,
                ModItems.MAT_025, 1, ModItems.MAT_163, 1,
                ModItems.REN_DONG_HUA, 1, ModItems.MAT_084, 10);

        // 序列7·纵火家：火蝾螈的腺体 + 岩浆精灵的核心 + 火蝾螈的血液 + 岩浆桶x10 + 红冠凤仙草x10
        add(ModItems.PYROMANIAC_POTION,
                ModItems.MAT_099, 1, ModItems.MAT_030, 1, ModItems.MAT_100, 1,
                Items.LAVA_BUCKET, 10, ModItems.MAT_144, 10);

        // 序列6·阴谋家：狩猎黑蛛的复眼 + 人身狮的大脑 + 狩猎黑蛛的毒腺 + 人身狮的血液 + 白桦树树苗x2
        add(ModItems.CONSPIRER_POTION,
                ModItems.MAT_117, 1, ModItems.REN_SHEN_SHI_DE_DA_NAO, 1,
                ModItems.MAT_118, 1, ModItems.REN_SHEN_SHI_DE_XUE_YE, 1, Items.BIRCH_SAPLING, 2);

        // ===== 魔女途径 =====

        // 序列9·刺客：阴影毒花 + 蛇身怪鸟的黑羽 + 阴影毒花的花瓣x3 + 蜘蛛眼
        add(ModItems.ASSASSIN_POTION,
                ModItems.MAT_210, 1, ModItems.MAT_172, 1, ModItems.MAT_211, 3, Items.SPIDER_EYE, 1);

        // 序列8·教唆者：魔喉蜜䴕的心脏 + 黑暗潜伏者毒囊 + 魔喉蜜䴕的鸣管 + 蓝色曼陀罗x3 + 水蕨草 + 纯水
        add(ModItems.INSTIGATOR_POTION,
                ModItems.MAT_231, 1, ModItems.MAT_243, 1, ModItems.MAT_232, 1,
                ModItems.MAT_269, 3, ModItems.MAT_084, 1, ModItems.PURIFIED_WATER, 1);

        // 序列7·女巫：黑渊魔鱼的全部血液 + 纯水 + 金色曼陀罗花汁 + 阴影蜥蜴的鳞片x3 + 水仙花汁液x10
        add(ModItems.WITCH_POTION,
                ModItems.MAT_245, 1, ModItems.PURIFIED_WATER, 1, ModItems.MAT_192, 1,
                ModItems.MAT_212, 3, ModItems.MAT_081, 10);

        // 序列6·欢愉魔女：魅欲女妖的眼睛 + 寡妇巨蛛的丝腺 + 纯水 + 黑色曼陀罗汁液x5 + 魅欲女妖残留的全部毛发
        add(ModItems.JOYFUL_WITCH_POTION,
                ModItems.MAT_230, 1, ModItems.MAT_053, 1, ModItems.PURIFIED_WATER, 1,
                ModItems.MAT_248, 5, ModItems.MAT_273, 1);

        // ===== 黑皇帝途径 =====

        // 序列9·律师：知识之书 + 迷宫鹦鹉的舌头 + 迷宫鹦鹉的血液 + 金合欢木 + 白茉莉
        add(ModItems.BLACK_EMPEROR_9_POTION,
                ModItems.MAT_137, 1, ModItems.MAT_181, 1, ModItems.MAT_182, 1,
                Items.ACACIA_LOG, 1, ModItems.MAT_129, 1);

        // 序列8·野蛮人：狂化草 + 大地犀牛的实心独角结晶 + 香蜂草 + 烈酒
        add(ModItems.BLACK_EMPEROR_8_POTION,
                ModItems.MAT_113, 1, ModItems.MAT_020, 1, ModItems.MAT_226, 1, ModItems.MAT_110, 1);

        // 序列7·贿赂者：怪脸大麻结晶 + 金色曼陀罗花汁x5 + 黑色曼陀罗汁液x5 + 迷幻草x4 + 红葡萄酒
        add(ModItems.BLACK_EMPEROR_7_POTION,
                ModItems.MAT_049, 1, ModItems.MAT_192, 5, ModItems.MAT_248, 5,
                ModItems.PSYCHEDELIC_GRASS, 4, ModItems.MAT_149, 1);

        // 序列6·腐化男爵：人脸狒狒的鼻子 + 尸花（腐烂尸花）+ 人脸狒狒的血液 + 灰琥珀x10 + 烈酒 + 铅粉
        add(ModItems.BLACK_EMPEROR_6_POTION,
                ModItems.MAT_258, 1, ModItems.MAT_027, 1, ModItems.REN_LIAN_FEI_FEI_DE_XUE_YE, 1,
                ModItems.MAT_101, 10, ModItems.MAT_110, 1, ModItems.MAT_196, 1);

        // ===== 审判者途径 =====

        // 序列9·仲裁人：白鬃狩猎者的面部皮肤 + 皇冠猎隼的三根尾羽 + 白鬃狩猎者的血液 + 皇冠猎隼的鸟爪 + 黄金
        add(ModItems.JUSTICE_9_POTION,
                ModItems.MAT_132, 1, ModItems.MAT_135, 1, ModItems.MAT_131, 1,
                ModItems.MAT_136, 1, Items.GOLD_INGOT, 1);

        // 序列8·治安官：恐惧魔虫的眼睛 + 银白战熊的右掌 + 银白战熊的血液 + 恐惧魔虫的汁液 + 任命文书
        add(ModItems.JUSTICE_8_POTION,
                ModItems.MAT_051, 1, ModItems.MAT_197, 1, ModItems.MAT_198, 1,
                ModItems.MAT_050, 1, ModItems.REN_MING_WEN_SHU, 1);

        // 序列7·审讯者：闪纹黑蛇的角 + 湖中之灵的粉尘 + 闪纹黑蛇的角的血液 + 被闪电劈焦的木头
        add(ModItems.JUSTICE_7_POTION,
                ModItems.MAT_208, 1, ModItems.MAT_098, 1, ModItems.MAT_209, 1, ModItems.SCORCHED_WOOD, 1);

        // 序列6·法官：黑域领主的头皮 + 异纹巨虎的尾巴 + 黑域领主的血液 + 异纹巨虎的牙齿x2 + 凋零玫瑰
        add(ModItems.JUSTICE_6_POTION,
                ModItems.MAT_240, 1, ModItems.MAT_046, 1, ModItems.MAT_275, 1,
                ModItems.MAT_047, 2, Items.WITHER_ROSE, 1);

        // ===== 黑暗途径 =====

        // 序列9·不眠者：虞美人 + 六足猫头鹰眼睛 + 烈酒 + 夜香草x10 + 可可豆
        add(ModItems.SLEEPLESS_POTION,
                Items.POPPY, 1, ModItems.MAT_260, 1, ModItems.MAT_110, 1,
                ModBlocks.NIGHT_FRAGRANCE, 10, Items.COCOA_BEANS, 1);

        // 序列8·午夜诗人：红月咆哮者的声带 + 摄魂风铃花 + 红葡萄酒 + 红月咆哮者的毛发x7 + 泥土
        add(ModItems.MIDNIGHT_POET_POTION,
                ModItems.MAT_145, 1, ModItems.SHE_HUN_FENG_LING_HUA, 1,
                ModItems.MAT_149, 1, ModItems.MAT_266, 7, Items.DIRT, 1);

        // 序列7·梦魇：食梦黑鸦的心脏 + 噩梦之影 + 纯水 + 食梦黑鸦的幻羽 + 月亮花
        add(ModItems.NIGHTMARE_POTION,
                ModItems.MAT_222, 1, ModItems.MAT_011, 1, ModItems.PURIFIED_WATER, 1,
                ModItems.MAT_221, 1, ModItems.MAT_263, 1);

        // 序列6·安魂师：腐烂牧者的灵体结晶 + 异域深眠者的头骨 + 被祝福的圣水 + 腐烂牧者的脓液x7 + 深眠花
        add(ModItems.REQUIEM_POTION,
                ModItems.MAT_109, 1, ModItems.MAT_045, 1, ModItems.MAT_180, 1,
                ModItems.MAT_276, 7, ModItems.SHEN_MIAN_HUA, 1);

        // ===== 死神途径 =====

        // 序列9·收尸人：黑斑青蛙肉布套人的结晶 + 烈酒 + 黑斑青蛙的体液x10 + 黑色百合纯露x10
        add(ModItems.CORPSE_COLLECTOR_POTION,
                ModItems.MAT_242, 1, ModItems.MAT_110, 1, ModItems.MAT_241, 10, ModItems.MAT_249, 10);

        // 序列8·掘墓人：墓园血玫瑰 + 告死乌鸦的眼珠 + 酒精 + 告死乌鸦的羽毛x3 + 血玫瑰的叶子x5
        add(ModItems.GRAVEDIGGER_POTION,
                ModItems.MU_YUAN_XUE_MEI_GUI, 1, ModItems.MAT_006, 1, ModItems.MAT_188, 1,
                ModItems.MAT_007, 3, ModItems.MAT_175, 5);

        // 序列7·通灵者：幽灵怪猫的前爪 + 灵界水晶 + 幽灵怪猫的血液 + 死于恶灵附身之人的脑浆
        add(ModItems.SPIRIT_MEDIUM_POTION,
                ModItems.MAT_043, 1, ModItems.MAT_104, 1, ModItems.MAT_044, 1, ModItems.MAT_078, 1);

        // 序列6·死灵导师：死亡之影 + 苍白巫妖的魂体 + 苍白巫妖的碎骨 + 死亡之影的寄居之物 + 徘徊的幽灵
        add(ModItems.NECROMANCER_POTION,
                ModItems.MAT_079, 1, ModItems.MAT_161, 1, ModItems.MAT_160, 1,
                ModItems.MAT_080, 1, ModItems.MAT_048, 1);

        // ===== 黄昏巨人途径 =====

        // 序列9·战士：巨人战士的肌肉核 + 嗜血犀牛的角 + 巨人战士的血液 + 巨人战士的心脏
        add(ModItems.WARRIOR_POTION,
                ModItems.MAT_035, 1, ModItems.MAT_010, 1, ModItems.MAT_036, 1, ModItems.MAT_034, 1);

        // 序列8·格斗家：巨人侍从的掌骨 + 森林游荡者的中指 + 巨人侍从的血液 + 巨人侍从的胆囊 + 紫水晶
        add(ModItems.FIGHTER_POTION,
                ModItems.MAT_031, 1, ModItems.MAT_076, 1, ModItems.MAT_033, 1,
                ModItems.MAT_032, 1, Items.AMETHYST_SHARD, 1);

        // 序列7·武器大师：蓝巨人的脊椎 + 六臂灰猩的耳朵 + 蓝巨人的血液 + 蓝巨人的头发x10
        add(ModItems.WEAPON_MASTER_POTION,
                ModItems.MAT_165, 1, ModItems.MAT_259, 1, ModItems.MAT_166, 1, ModItems.MAT_277, 10);

        // 序列6·黎明骑士：晨曦巨人的血液结晶 + 银色巨熊的右掌 + 晨曦巨人的脑髓液 + 晨曦巨人的指甲x3 + 圣水
        add(ModItems.DAWN_KNIGHT_POTION,
                ModItems.MAT_065, 1, ModItems.MAT_201, 1, ModItems.MAT_064, 1,
                ModItems.MAT_063, 3, ModItems.MAT_016, 1);

        // ===== 命运之轮途径 =====

        // 序列9·怪物：鲤鱼的尾巴 + 银色四叶草 + 浑浊的水 + 普通四叶草x3 + 幸运符 + 自身血液x9
        add(ModItems.WHEEL_9_POTION,
                ModItems.MAT_234, 1, ModItems.MAT_272, 1, ModItems.MAT_090, 1,
                ModItems.MAT_262, 3, ModItems.MAT_039, 1, ModItems.ZI_SHEN_XUE_YE, 9);

        // 序列8·机器：橘光石 + 幼年独角兽的结晶 + 幼年独角兽的血液 + 蛇鳞x10 + 白栗花x7 + 铁粒
        add(ModItems.WHEEL_8_POTION,
                ModItems.MAT_077, 1, ModItems.MAT_041, 1, ModItems.MAT_042, 1,
                ModItems.MAT_270, 10, ModItems.MAT_128, 7, Items.IRON_NUGGET, 1);

        // 序列7·幸运儿：好运之花 + 银眼怪蛇的眼珠 + 银眼怪蛇的血液 + 好运之花生长的泥土 + 金币（以便士替代）
        add(ModItems.WHEEL_7_POTION,
                ModItems.MAT_022, 1, ModItems.MAT_199, 1, ModItems.MAT_200, 1,
                ModItems.LUCKY_FLOWER_SOIL, 1, ModItems.PENNY, 1);

        // 序列6·灾祸教士：蓝斑火鸟的心脏 + 独眼白牛的蛇尾 + 独眼白牛的血液 + 蓝斑火鸟的羽毛x3 + 龙胆草 + 石头
        add(ModItems.WHEEL_6_POTION,
                ModItems.MAT_169, 1, ModItems.MAT_119, 1, ModItems.MAT_120, 1,
                ModItems.MAT_170, 3, ModItems.MAT_256, 1, Items.STONE, 1);

        // ===== 隐者途径 =====

        // 序列9·窥秘人：噩梦邪眼的角膜 + 噩梦邪眼的脓液 + 疯人院入院记录（记载他人秘密的纸）+ 迷迭香
        add(ModItems.HERMIT_9_POTION,
                ModItems.MAT_013, 1, ModItems.MAT_012, 1, ModItems.ASYLUM_RECORD, 1, ModItems.MAT_184, 1);

        // 序列8·格斗学者：铁臂兽人的脊椎 + 绿龟的心脏 + 铁臂兽人的血液 + 绿龟的背壳
        add(ModItems.HERMIT_8_POTION,
                ModItems.MAT_194, 1, ModItems.MAT_154, 1, ModItems.MAT_195, 1, ModItems.MAT_267, 1);

        // 序列7·巫师：鹿头兽人的角 + 灰鸟祖母的眼珠 + 鹿头兽人的血液 + 灰鸟祖母的羽毛x9
        add(ModItems.HERMIT_7_POTION,
                ModItems.MAT_236, 1, ModItems.MAT_102, 1, ModItems.MAT_235, 1, ModItems.MAT_103, 9);

        // 序列6·卷轴教授：树人祭司的树心 + 纸妖的虚幻之眼 + 树人祭司的汁液 + 纸妖的灰烬 + 高阶卷轴
        add(ModItems.HERMIT_6_POTION,
                ModItems.MAT_074, 1, ModItems.MAT_153, 1, ModItems.MAT_075, 1,
                ModItems.MAT_152, 1, ModItems.MAT_229, 1);

        // ===== 完美者途径 =====

        // 序列9·通识者：高山雪人的脑核 + 高山雪人的血液 + 书册
        add(ModItems.PARAGON_9_POTION,
                ModItems.MAT_228, 1, ModItems.GAO_SHAN_XUE_REN_DE_XUE_YE, 1, ModItems.SHU_CE, 1);

        // 序列8·考古学家：古墓幽影的结晶 + 遗迹徘徊者的脊髓 + 古墓幽影的粉尘 + 遗迹徘徊者的血液 + 古代文献 + 有少许神秘力量的古代物品
        add(ModItems.PARAGON_8_POTION,
                ModItems.MAT_004, 1, ModItems.MAT_186, 1, ModItems.MAT_003, 1,
                ModItems.MAT_187, 1, ModItems.MAT_002, 1, ModItems.YOU_SHAO_XU_SHEN_MI_LI_LIANG_DE_GU_DAI_WU_PIN, 1);

        // 序列7·鉴定师：灵界邪眼 + 灵界邪眼的泪水x9 + 书籍 + 薄荷 + 人皮古卷
        add(ModItems.PARAGON_7_POTION,
                ModItems.MAT_105, 1, ModItems.MAT_106, 9, ModItems.SHU_JI, 1,
                ModItems.MAT_171, 1, ModItems.REN_PI_GU_JUAN, 1);

        // 序列6·机械专家：四臂疯人的大脑 + 四臂疯人的血液 + 亲手打磨的齿轮 + 新构想的且没有主要错误的机械设计图
        add(ModItems.PARAGON_6_POTION,
                ModItems.MAT_014, 1, ModItems.MAT_015, 1, ModItems.QIN_SHOU_DA_MO_DE_CHI_LUN, 1,
                ModItems.XIN_GOU_XIANG_DE_QIE_MEI_YOU_ZHU_YAO_CUO_WU_DE_JI_XIE_SHE_JI_TU, 1);

        // ===== 母亲途径 =====

        // 序列9·耕种者：金毛巨猩的大脑 + 告雨鸟的眼珠 + 金色巨猩的血液 + 告雨鸟的羽毛x3 + 麦子 + 面包
        add(ModItems.MOTHER_9_POTION,
                ModItems.MAT_190, 1, ModItems.MAT_008, 1, ModItems.MAT_191, 1,
                ModItems.MAT_009, 3, Items.WHEAT, 1, Items.BREAD, 1);

        // 序列8·医师：白桦木 + 治疗药水x10 + 橡树树叶（不同树叶）+ 奶桶 + 药膏
        add(ModItems.MOTHER_8_POTION,
                Items.BIRCH_LOG, 1, ModItems.MAT_085, 10, Items.OAK_LEAVES, 1,
                Items.MILK_BUCKET, 1, ModItems.MAT_162, 1);

        // 序列7·丰收祭司：丰收巨人的肾脏 + 橡树树叶 + 橡树树苗 + 橡木 + 奶桶 + 丰收巨人的生殖器官
        add(ModItems.MOTHER_7_POTION,
                ModItems.MAT_185, 1, Items.OAK_LEAVES, 1, Items.OAK_SAPLING, 1,
                Items.OAK_LOG, 1, Items.MILK_BUCKET, 1, ModItems.MAT_066, 1);

        // 序列6·生物学家：丰收巨人学者的大脑 + 食毒蝙蝠的胃袋 + 食毒蝙蝠的血液 + 丰收巨人学者的舌尖 + 蛋
        add(ModItems.MOTHER_6_POTION,
                ModItems.FENG_SHOU_JU_REN_XUE_ZHE_DE_DA_NAO, 1, ModItems.MAT_223, 1,
                ModItems.MAT_224, 1, ModItems.FENG_SHOU_JU_REN_XUE_ZHE_DE_SHE_JIAN, 1, Items.EGG, 1);

        // ===== 月亮途径 =====

        // 序列9·药师：皇冠水母的髓制结晶 + 成年独角飞马的角 + 成年独角飞马的血液 + 皇冠水母的触须x7
        add(ModItems.MOON_9_POTION,
                ModItems.MAT_134, 1, ModItems.MAT_056, 1, ModItems.MAT_055, 1, ModItems.MAT_133, 7);

        // 序列8·驯兽师：精灵之泉的髓质结晶 + 精灵之泉的泉水 + 野茄提炼的汁液x10 + 动物尸油
        add(ModItems.MOON_8_POTION,
                ModItems.MAT_141, 1, ModItems.MAT_140, 1,
                ModItems.YE_JIA_TI_LIAN_DE_ZHI_YE, 10, ModItems.DONG_WU_SHI_YOU, 1);

        // 序列7·吸血鬼：血源精华 + 渴血兽的血液 + 兔皮
        add(ModItems.MOON_7_POTION,
                ModItems.MAT_174, 1, ModItems.MAT_097, 1, ModItems.TU_PI, 1);

        // 序列6·魔药教授：巨怪智者的大脑 + 黑暗蝙蝠的翅膀 + 巨怪智者的血液 + 自身制造的药剂
        add(ModItems.MOON_6_POTION,
                ModItems.MAT_037, 1, ModItems.MAT_244, 1, ModItems.MAT_038, 1,
                ModItems.ZI_SHEN_ZHI_ZAO_DE_YAO_JI, 1);

        // ===== 被缚者途径 =====

        // 序列9·囚犯：伪人的血液之源 + 肮脏的水 + 伪人的头发x10
        add(ModItems.CHAINED_9_POTION,
                ModItems.WEI_REN_DE_XUE_YE_ZHI_YUAN, 1, ModItems.MAT_158, 1, ModItems.WEI_REN_DE_TOU_FA, 10);

        // 序列8·疯子：变形怪的大脑 + 食人之犬的舌头 + 自身的鲜血 + 食人之犬的唾液x13
        add(ModItems.CHAINED_8_POTION,
                ModItems.MAT_001, 1, ModItems.MAT_218, 1, ModItems.ZI_SHEN_XUE_YE, 1, ModItems.MAT_217, 13);

        // 序列7·狼人：狼人的獠牙 + 狼人的血液 + 狼人的黑毛x10 + 人类的内脏
        add(ModItems.CHAINED_7_POTION,
                ModItems.MAT_124, 1, ModItems.MAT_122, 1, ModItems.MAT_123, 10, ModItems.REN_LEI_DE_NEI_ZANG, 1);

        // 序列6·活尸：活尸的心脏 + 活人的鲜血 + 活尸的血肉 + 活尸的眼珠 + 月亮花x3
        add(ModItems.CHAINED_6_POTION,
                ModItems.MAT_088, 1, ModItems.MAT_087, 1, ModItems.MAT_089, 1,
                ModItems.MAT_264, 1, ModItems.MAT_263, 3);

        // ===== 深渊途径 =====

        // 序列9·罪犯：「凶蛮之犬」的脑袋 + 「杀人黑鸦」的晶羽 + 「凶蛮之犬」的血液 + 「杀人黑鸦」的血液
        add(ModItems.ABYSS_9_POTION,
                ModItems.XIONG_MAN_ZHI_QUAN_DE_NAO_DAI, 1, ModItems.MAT_072, 1,
                ModItems.XIONG_MAN_ZHI_QUAN_DE_XUE_YE, 1, ModItems.MAT_073, 1);

        // 序列8·冷血者：「鼠群之主」的尸骸 + 「无皮血猫」的心脏 + 「鼠群之主」的血液 + 「无皮血猫」的血液 + 自身谋害的第一个同种族者的心脏
        add(ModItems.ABYSS_8_POTION,
                ModItems.MAT_252, 1, ModItems.MAT_061, 1, ModItems.MAT_253, 1,
                ModItems.MAT_062, 1, ModItems.MAT_268, 1);

        // 序列7·连环杀手：「食人秃鹫」的喙 + 「暗影之蛇」的尖牙 + 「食人秃鹫」的血液 + 「暗影之蛇」的血液 + 亲手所杀之人的完整外皮
        add(ModItems.ABYSS_7_POTION,
                ModItems.MAT_219, 1, ModItems.MAT_067, 1, ModItems.MAT_220, 1,
                ModItems.MAT_068, 1, ModItems.MAT_257, 1);

        // 序列6·恶魔：「红眼邪豹」的舌头 + 「岩浆之魔」的独角 + 「红眼邪豹」的血液 + 「岩浆之魔」的熔融之液 + 黑百合
        add(ModItems.ABYSS_6_POTION,
                ModItems.MAT_146, 1, ModItems.MAT_029, 1, ModItems.MAT_147, 1,
                ModItems.MAT_028, 1, ModItems.MAT_247, 1);
    }

    /**
     * 紧凑注册：奇数位为材料（物品 / 方块 / 供应商），偶数位为数量。
     * 配方 ID 取产物魔药的注册名 path；产物为惰性提供，注册阶段不触碰注册表实例。
     */
    private static void add(Supplier<? extends Item> result, Object... pairs) {
        Map<Item, Integer> ingredients = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            ingredients.merge(resolveItem(pairs[i]), (Integer) pairs[i + 1], Integer::sum);
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.get()).getPath();
        register(new BrewingRecipe(id, ingredients, () -> new ItemStack(result.get())));
    }

    /** 解析材料引用：物品 / 方块 / 延迟供应商（供应商可提供方块或物品）。 */
    private static Item resolveItem(Object ref) {
        if (ref instanceof Item item) {
            return item;
        }
        if (ref instanceof Block block) {
            return block.asItem();
        }
        if (ref instanceof Supplier<?> supplier) {
            Object value = supplier.get();
            if (value instanceof Block block) {
                return block.asItem();
            }
            if (value instanceof Item item) {
                return item;
            }
        }
        throw new IllegalArgumentException("无法解析的材料引用: " + ref);
    }
}
