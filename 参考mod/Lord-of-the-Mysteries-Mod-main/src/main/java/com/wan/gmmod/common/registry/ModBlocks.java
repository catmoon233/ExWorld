package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.block.GuimiPlantBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(GuimiMod.MODID);

    // 示例方块（无功能）
    public static final DeferredBlock<Block> SPIRIT_STONE =
            BLOCKS.register("spirit_stone",
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

    // ===== 主世界自然生成的草本植物（十字形模型，用于炼制魔药）=====

    /** 金薄荷：破坏掉落「金薄荷叶子」。 */
    public static final DeferredBlock<GuimiPlantBlock> GOLD_MINT =
            BLOCKS.register("gold_mint", () -> new GuimiPlantBlock(plantProperties()));

    /** 夜香草：破坏掉落自身。 */
    public static final DeferredBlock<GuimiPlantBlock> NIGHT_FRAGRANCE =
            BLOCKS.register("night_fragrance", () -> new GuimiPlantBlock(plantProperties()));

    /** 龙血草：破坏掉落自身。 */
    public static final DeferredBlock<GuimiPlantBlock> DRAGON_BLOOD_GRASS =
            BLOCKS.register("dragon_blood_grass", () -> new GuimiPlantBlock(plantProperties()));

    /** 毒堇：破坏掉落自身。 */
    public static final DeferredBlock<GuimiPlantBlock> POISON_HEMLOCK =
            BLOCKS.register("poison_hemlock", () -> new GuimiPlantBlock(plantProperties()));

    // ===== 仪式方块 =====

    /** 好运之花生长的泥土：蕴含好运力量的泥土方块。 */
    public static final DeferredBlock<Block> LUCKY_FLOWER_SOIL =
            BLOCKS.register("lucky_flower_soil",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DIRT)
                            .strength(1.5F)
                            .sound(SoundType.ROOTED_DIRT)
                            .requiresCorrectToolForDrops()));

    /** 被闪电劈焦的木头：被闪电击中的焦黑木头。 */
    public static final DeferredBlock<Block> SCORCHED_WOOD =
            BLOCKS.register("scorched_wood",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F)
                            .sound(SoundType.WOOD)
                            .requiresCorrectToolForDrops()));

    // ===== 好运圃植物（十字形，世界生成）=====

    /** 好运之花：好运圃中心的稀有花朵，生长于好运之花泥土之上，破坏掉落「好运之花」材料。 */
    public static final DeferredBlock<GuimiPlantBlock> LUCKY_FLOWER =
            BLOCKS.register("lucky_flower", () -> new GuimiPlantBlock(plantProperties()));

    /** 普通四叶草：好运圃中环绕好运之花生长的四叶草，破坏掉落「普通四叶草」材料。 */
    public static final DeferredBlock<GuimiPlantBlock> FOUR_LEAF_CLOVER =
            BLOCKS.register("four_leaf_clover", () -> new GuimiPlantBlock(plantProperties()));

    /** 银色四叶草：四叶草中的小概率变异，破坏掉落「银色四叶草」材料。 */
    public static final DeferredBlock<GuimiPlantBlock> SILVER_FOUR_LEAF_CLOVER =
            BLOCKS.register("silver_four_leaf_clover", () -> new GuimiPlantBlock(plantProperties()));

    // ===== 可世界生成的花草（26 种，破坏掉落同名材料物品）=====

    /** 可生成的花草方块注册表：方块名 = 材料物品名 + "_plant"。 */
    public static final java.util.Map<String, DeferredBlock<GuimiPlantBlock>> GENERATED_PLANTS =
            new java.util.LinkedHashMap<>();

    /** 可生成的花草方块名列表（对应战利品表掉落同名去 _plant 后缀的材料物品）。 */
    private static final String[] GENERATED_PLANT_NAMES = {
            "material_130_plant", "material_261_plant", "jin_bian_tai_yang_hua_plant",
            "material_127_plant", "material_128_plant", "material_148_plant",
            "material_226_plant", "material_227_plant", "material_171_plant",
            "material_184_plant", "zhong_xia_cao_plant", "material_156_plant",
            "material_084_plant", "material_113_plant", "psychedelic_grass_plant",
            "material_271_plant", "material_129_plant", "material_092_plant",
            "material_144_plant", "material_269_plant", "material_263_plant",
            "mu_yuan_xue_mei_gui_plant", "material_256_plant", "ren_dong_hua_plant",
            "ren_mian_long_cao_plant", "material_247_plant"
    };

    static {
        for (String name : GENERATED_PLANT_NAMES) {
            GENERATED_PLANTS.put(name, BLOCKS.register(name, () -> new GuimiPlantBlock(plantProperties())));
        }
    }

    /** 祭台：仪式系统核心方块，右键放置材料合成魔药，支持冥想交互。 */
    public static final DeferredBlock<Block> ALTAR =
            BLOCKS.register("altar",
                    () -> new com.wan.gmmod.common.block.AltarBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.DEEPSLATE)
                                    .strength(3.0F, 6.0F)
                                    .sound(SoundType.DEEPSLATE)
                                    .requiresCorrectToolForDrops()));

    /** 花草类通用方块属性：无碰撞、瞬间破坏、草声、XZ 随机偏移、活塞可摧毁。 */
    private static BlockBehaviour.Properties plantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY);
    }
}
