package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

public class ModCreativeTabs {

    /**
     * 植物 / 花草 / 树木产物的物品注册名 path 统一清单：
     * 植物标签页按此全量收录，主标签页的拼音材料自动过滤按此排除，保证分类一致。
     */
    private static final Set<String> PLANT_PATHS = Set.of(
            // 植物方块（含方块物品）
            "gold_mint", "night_fragrance", "dragon_blood_grass", "poison_hemlock", "lucky_flower_soil",
            "lucky_flower", "four_leaf_clover", "silver_four_leaf_clover",
            // 命名花草材料
            "gold_mint_leaf", "agrimony", "black_edged_sunflower", "black_mandragora",
            "golden_cloak_grass", "mandragora", "psychedelic_grass", "face_rose",
            "mist_treant_root", "mist_treant_juice", "dragon_pattern_tree_bark",
            // 花朵 / 花瓣 / 花汁（material 编号）
            "material_022", "material_026", "material_027", "material_081", "material_092",
            "material_111", "material_127", "material_128", "material_129", "material_130",
            "material_139", "material_142", "material_143", "material_144", "material_148",
            "material_150", "material_151", "material_175", "material_178", "material_183",
            "material_192", "material_210", "material_211", "material_247", "material_248",
            "material_249", "material_261", "material_262", "material_263", "material_269",
            "material_271", "material_272",
            // 草 / 叶 / 藤（material 编号）
            "material_083", "material_084", "material_113", "material_156", "material_157",
            "material_171", "material_184", "material_226", "material_227", "material_256",
            // 树木 / 树人产物（material 编号）
            "material_074", "material_075", "material_189", "material_205", "material_206",
            "material_207",
            // 无 material 版的独立拼音物品
            "zhong_xia_cao",
            // 配方文档补充的花草
            "ren_mian_long_cao", "mu_yuan_xue_mei_gui", "ren_dong_hua",
            "she_hun_feng_ling_hua", "shen_mian_hua", "jin_bian_tai_yang_hua");

    /**
     * 拼音名重复物品（与 material 编号同物同图，仅 ID 不同）：
     * 不在植物标签页展示（避免重复），但主标签页同样需要排除。
     */
    private static final Set<String> PINYIN_PLANT_DUPS = Set.of(
            "tai_yang_hua", "yue_liang_hua", "yin_se_si_ye_cao", "xiang_feng_cao",
            "shui_jue_cao", "shui_jue_zhi_ye", "shui_xian_hua_zhi_ye", "xue_mei_gui_de_ye_zi",
            "xue_xing_hua_su", "ye_jia_ti_lian_de_zhi_ye", "yin_ying_du_hua",
            "yin_ying_du_hua_de_hua_ban", "zhang_zhe_zhi_shu_de_gen_jing_jie_jing",
            "zhang_zhe_zhi_shu_de_guo_shi", "zhang_zhe_zhi_shu_de_shu_pi",
            "shu_ren_ji_si_de_shu_xin", "shu_ren_ji_si_de_zhi_ye", "yang_xu_cao");
    // 注意：创造标签页注册表是 Registries.CREATIVE_MODE_TAB，不是 ITEM
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GuimiMod.MODID);

    public static final Supplier<CreativeModeTab> GUIMI_TAB = TABS.register("guimi_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guimi_mod.guimi_tab"))
                    .icon(() -> new ItemStack(ModItems.SEER_POTION.get()))
                    .displayItems((params, output) -> {
                        // 在这里添加所有属于本模组的物品

                        output.accept(ModItems.SEER_POTION.get());
                        output.accept(ModItems.TOP_HAT.get());
                        output.accept(ModItems.WAND.get());
                        output.accept(ModItems.PENDULUM.get());
                        output.accept(ModItems.VEST.get());
                        output.accept(ModItems.BLACK_VEST.get());
                        output.accept(ModItems.LONG_PANTS.get());
                        output.accept(ModItems.BLACK_SHOES.get());
                        output.accept(ModItems.SEALED_ARTIFACT.get());
                        output.accept(ModItems.MAGIC_ARTIFACT.get());
                        // 仪式 / 武器
                        output.accept(ModItems.RITUAL_DAGGER.get());
                        output.accept(ModItems.SILENCE_GUN.get());
                        // 灵性钓竿
                        output.accept(ModItems.SPIRIT_ROD.get());
                        // 纸人（纸人替身能力锚点）
                        output.accept(ModItems.PAPER_FIGURINE.get());
                        // 纸牌（小丑「飞牌」专属弹药）
                        output.accept(ModItems.PAPER_CARD.get());
                        // 子弹
                        output.accept(ModItems.BULLET.get());
                        output.accept(ModItems.DEPRIVATION_BULLET.get());
                        output.accept(ModItems.PARASITIC_BULLET.get());
                        output.accept(ModItems.SPIRIT_CONTROL_BULLET.get());
                        output.accept(ModItems.DECEPTION_BULLET.get());
                        output.accept(ModItems.EXORCISM_BULLET.get());
                        output.accept(ModItems.PURIFICATION_BULLET.get());
                        // 炼药材料（非植物类）
                        output.accept(ModItems.STAR_CRYSTAL.get());
                        output.accept(ModItems.LAVA_OCTOPUS_BLOOD.get());
                        output.accept(ModItems.THOUSAND_FACED_HUNTER_BLOOD.get());
                        output.accept(ModItems.THOUSAND_FACED_HUNTER_PITUITARY.get());
                        output.accept(ModItems.HORNACIS_GOAT_HORN_CRYSTAL.get());
                        output.accept(ModItems.DEEP_SEA_NAGA_HAIR.get());
                        output.accept(ModItems.WATER_SHAPE_GEM.get());
                        output.accept(ModItems.EVIL_PANTHER_SPINAL_FLUID.get());
                        output.accept(ModItems.ANCIENT_WRAITH_DUST.get());
                        output.accept(ModItems.SIX_WINGED_GARGOYLE_CORE_CRYSTAL.get());
                        output.accept(ModItems.SONIA_GOLDEN_SPRING_WATER.get());
                        output.accept(ModItems.DRAGON_PATTERN_TREE_BARK.get());
                        output.accept(ModItems.ANCIENT_WRAITH_RESIDUAL_SPIRITUALITY.get());
                        output.accept(ModItems.SIX_WINGED_GARGOYLE_EYE.get());
                        // 96个pinyin名材料物品（植物类已移至植物标签页）
                        ModItems.ITEMS.getEntries().stream()
                                .filter(entry -> entry.getId().getPath().matches("^[a-z_]+$") && entry.getId().getPath().length() > 10)
                                .filter(entry -> !entry.getId().getPath().contains("potion"))
                                .filter(entry -> !entry.getId().getPath().contains("spawn_egg"))
                                .filter(entry -> !entry.getId().getPath().contains("bullet"))
                                .filter(entry -> !entry.getId().getPath().contains("talisman"))
                                .filter(entry -> !entry.getId().getPath().startsWith("material_"))
                                .filter(entry -> !PLANT_PATHS.contains(entry.getId().getPath()))
                                .filter(entry -> !PINYIN_PLANT_DUPS.contains(entry.getId().getPath()))
                                .forEach(entry -> output.accept(entry.get()));
                        // 祭台
                        output.accept(ModItems.ALTAR.get());
                        // 魔女途径
                        output.accept(ModItems.MIRROR.get());
                        output.accept(ModItems.MIRROR_BROKEN.get());
                        // 战争之红途径
                        output.accept(ModItems.HEMOSTATIC_SALVE.get());
                        output.accept(ModItems.BASIC_POISON.get());
                        output.accept(ModItems.FLAME_WEAPON.get());
                        // 灵性符咒
                        output.accept(ModItems.BLANK_TALISMAN.get());
                        output.accept(ModItems.PURIFICATION_TALISMAN.get());
                        output.accept(ModItems.REQUIEM_TALISMAN.get());
                        output.accept(ModItems.ELECTRIC_TALISMAN.get());
                        // 货币
                        output.accept(ModItems.PENNY.get());
                        output.accept(ModItems.SOYLE.get());
                        output.accept(ModItems.GOLD_POUND.get());
                        // 黎明骑士装备
                        output.accept(ModItems.DAWN_SWORD.get());
                        output.accept(ModItems.DAWN_ARMOR.get());
                        output.accept(ModItems.MAGMA_SWORD.get());
                        // 古代神秘物品（有少许神秘力量的古代物品系列）
                        output.accept(ModItems.BROKEN_ICON_FINGER.get());
                        output.accept(ModItems.ASYLUM_RECORD.get());
                        output.accept(ModItems.SCORCHED_ROBE_FRAGMENT.get());
                        output.accept(ModItems.BLOODSTAINED_SIXPENCE.get());
                        // 材料物品（自动添加，植物类已移至植物标签页）
                        ModItems.ITEMS.getEntries().stream()
                                .filter(entry -> entry.getId().getPath().startsWith("material_"))
                                .filter(entry -> !PLANT_PATHS.contains(entry.getId().getPath()))
                                .forEach(entry -> output.accept(entry.get()));
                    })
                    .build()
    );

    /** 植物专属标签页：植物方块、花草、花瓣、树木产物等炼药植物材料。 */
    public static final Supplier<CreativeModeTab> PLANT_TAB = TABS.register("guimi_plants_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guimi_mod.guimi_plants"))
                    .icon(() -> new ItemStack(ModItems.NIGHT_FRAGRANCE.get()))
                    .displayItems((params, output) -> {
                        // 全部植物由 PLANT_PATHS 统一驱动（方块、花草、花瓣、树木产物、拼音重复物品）
                        for (String path : PLANT_PATHS) {
                            acceptByPath(output, path);
                        }
                    })
                    .build()
    );

    /** 魔药专属标签页：全部序列魔药、配方卷轴与炼药基底。 */
    public static final Supplier<CreativeModeTab> POTION_TAB = TABS.register("guimi_potions_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guimi_mod.guimi_potions"))
                    .icon(() -> new ItemStack(ModItems.SEER_POTION.get()))
                    .displayItems((params, output) -> {
                        // 炼药基底与药剂类
                        output.accept(ModItems.PURIFIED_WATER.get());
                        // 全部魔药（自动收录注册名含 potion 的物品）
                        ModItems.ITEMS.getEntries().stream()
                                .filter(entry -> entry.getId().getPath().contains("potion"))
                                .forEach(entry -> output.accept(entry.get()));
                        // 全部魔药配方卷轴
                        ModItems.RECIPE_SCROLLS.values().forEach(scroll -> output.accept(scroll.get()));
                    })
                    .build()
    );

    /** 生物蛋专属标签页：全部刷怪蛋。 */
    public static final Supplier<CreativeModeTab> SPAWN_EGG_TAB = TABS.register("guimi_spawn_eggs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guimi_mod.guimi_spawn_eggs"))
                    .icon(() -> new ItemStack(ModItems.WRAITH_SPAWN_EGG.get()))
                    .displayItems((params, output) ->
                            ModItems.ITEMS.getEntries().stream()
                                    .filter(entry -> entry.getId().getPath().contains("spawn_egg"))
                                    .forEach(entry -> output.accept(entry.get())))
                    .build()
    );

    /** 非凡特性专属标签页（各途径 / 序列的特性物品）。 */
    public static final Supplier<CreativeModeTab> CHARACTERISTIC_TAB = TABS.register("guimi_characteristics_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guimi_mod.guimi_characteristics"))
                    .icon(() -> new ItemStack(ModItems.CHARACTERISTIC.get()))
                    .displayItems((params, output) -> {
                        // 门途径序列 9 → 1 非凡特性（各有专属图标 door_seq_*）
                        for (int lvl = 9; lvl >= 1; lvl--) {
                            output.accept(CharacteristicItem.create(Sequences.Pathway.DOOR, lvl));
                        }
                        // 无数据默认特性 + 已实装的其他途径特性物品
                        output.accept(ModItems.CHARACTERISTIC.get());
                        output.accept(ModItems.FACELESS_CHARACTERISTIC.get());
                        output.accept(ModItems.MARIONETTIST_CHARACTERISTIC.get());
                        output.accept(ModItems.HUMAN_SKIN_SHADOW_CHARACTERISTIC.get());
                    })
                    .build()
    );

    /** 按注册名 path 从物品注册表中取出并加入标签页。 */
    private static void acceptByPath(CreativeModeTab.Output output, String... paths) {
        for (String path : paths) {
            ModItems.ITEMS.getEntries().stream()
                    .filter(entry -> entry.getId().getPath().equals(path))
                    .findFirst()
                    .ifPresent(entry -> output.accept(entry.get()));
        }
    }
}
