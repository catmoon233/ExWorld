package com.wan.gmmod.content.city;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wan.gmmod.GuimiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 廷根市生成器：服务器首次启动时在出生点附近生成城市地标。
 * <p>
 * 布局（正面朝北 -Z）：
 * <ul>
 *     <li><b>圣赛琳娜教堂</b>：单个完整结构 {@code jt}（78×96×66，已合并好最终朝向）；</li>
 *     <li><b>黑荆棘安保公司</b>：紧贴教堂东侧（地下室可通向教堂）；</li>
 *     <li><b>马路</b>：lu 结构铺成环绕教堂的环形大街（前后横街 + 左右竖街）。</li>
 * </ul>
 * 世界出生点随后被移到教堂正前方的大街上。城市只生成一次（以世界存档标记为准）。
 * <p>
 * <b>手动修正生成位置</b>：编辑数据包文件
 * {@code data/guimi_mod/city_layout.json}，其中每个结构给出
 * <b>中心坐标偏移</b>（相对城市原点，城市原点=出生点附近的 (ox, oy, oz)）与旋转。
 * 结构实际尺寸由生成器在运行时从结构 NBT 读取，保证始终以中心对齐，不依赖手填尺寸。
 * 修改后配合命令验证：{@code /guimi city layout} 打印规划、{@code /guimi city markers}
 * 放置标记方块、{@code /guimi city reset} 标记下次启动重新生成。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class LoenCityGenerator {

    private static final String SAVED_KEY = "guimi_loen_city_v11";

    /** 布局配置：服务器启动时从 city_layout.json 加载，命令与生成共用。 */
    private static CityLayout LAYOUT = new CityLayout();

    /** 重力方块 → 同色稳定方块：悬空时不会变成下落实体。 */
    private static final Map<Block, Block> GRAVITY_REPLACEMENTS = Map.ofEntries(
            Map.entry(Blocks.WHITE_CONCRETE_POWDER, Blocks.WHITE_CONCRETE),
            Map.entry(Blocks.ORANGE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE),
            Map.entry(Blocks.MAGENTA_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE),
            Map.entry(Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE),
            Map.entry(Blocks.YELLOW_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE),
            Map.entry(Blocks.LIME_CONCRETE_POWDER, Blocks.LIME_CONCRETE),
            Map.entry(Blocks.PINK_CONCRETE_POWDER, Blocks.PINK_CONCRETE),
            Map.entry(Blocks.GRAY_CONCRETE_POWDER, Blocks.GRAY_CONCRETE),
            Map.entry(Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE),
            Map.entry(Blocks.CYAN_CONCRETE_POWDER, Blocks.CYAN_CONCRETE),
            Map.entry(Blocks.PURPLE_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE),
            Map.entry(Blocks.BLUE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE),
            Map.entry(Blocks.BROWN_CONCRETE_POWDER, Blocks.BROWN_CONCRETE),
            Map.entry(Blocks.GREEN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE),
            Map.entry(Blocks.RED_CONCRETE_POWDER, Blocks.RED_CONCRETE),
            Map.entry(Blocks.BLACK_CONCRETE_POWDER, Blocks.BLACK_CONCRETE),
            Map.entry(Blocks.SAND, Blocks.SANDSTONE),
            Map.entry(Blocks.RED_SAND, Blocks.RED_SANDSTONE));

    private LoenCityGenerator() {}

    // ===== 默认布局（中心坐标偏移，与当前生效版本一致） =====

    private static Map<String, Entry> defaults() {
        Map<String, Entry> map = new LinkedHashMap<>();
        map.put("jt", new Entry("jt", 0, 48, 0, "none"));
        map.put("gongsi", new Entry("gongsi", 67, 11, -23, "cw180"));
        map.put("house_carson", new Entry("house_carson", -30, 25, -80, "none"));
        map.put("house_painted", new Entry("house_painted", 60, 11, -75, "none"));
        return map;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        LAYOUT = CityLayout.load(server.getResourceManager(), level);
    }

    /** Place Tingen at the given position. Does not move the world spawn. */
    public static void generateTingen(ServerLevel level, BlockPos around) {
        if (level == null || around == null) return;
        if (LAYOUT.entries().isEmpty() && level.getServer() != null) {
            LAYOUT = CityLayout.load(level.getServer().getResourceManager(), level);
        }
        generate(level, around);
    }

    private static void generate(ServerLevel level, BlockPos around) {
        int cx = around.getX();
        int cz = around.getZ();

        // 城市范围（含大街与预加载余量）
        int xMin = cx - 75;
        int xMax = cx + 105;
        int zMin = cz - 65;
        int zMax = cz + 150;

        // 先强制加载城市所有区块，否则 placeInWorld 的 setBlock 在未生成区块上会静默丢弃
        for (int chunkX = xMin >> 4; chunkX <= xMax >> 4; chunkX++) {
            for (int chunkZ = zMin >> 4; chunkZ <= zMax >> 4; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
        GuimiMod.LOGGER.info("廷根市生成：出生点 ({}, {}), 地表 Y={}", cx, cz, baseY);

        // 城市原点：教堂正门中心对准出生点（正门朝北 -Z）
        int ox = cx - 20;
        int oz = cz + 46;
        int oy = baseY;

        // 圣赛琳娜教堂 + 黑荆棘安保公司：按布局配置放置（中心坐标 + 旋转）
        for (Entry e : LAYOUT.entries()) {
            placeConfigured(level, e, ox, oy, oz);
        }

        // 马路：全部 lu 原样（宽 5 沿 X，路面从左往右 石砖-石头交替），沿 Z 向前铺成纵向大街
        // 正门前中央主街（向北延伸）
        for (int z = oz - 54; z >= oz - 118; z -= 13) {
            place(level, "lu", ox + 20, oy, z);
        }
        // 教堂南街（向南延伸）
        for (int z = oz + 47; z <= oz + 99; z += 13) {
            place(level, "lu", ox + 20, oy, z);
        }
        // 西街 / 东街（南北贯穿）
        for (int z = oz - 54; z <= oz + 47; z += 13) {
            place(level, "lu", ox - 49, oy, z);
            place(level, "lu", ox + 117, oy, z);
        }

        // 路灯：沿各条马路每隔 26 格居中放一盏 ludeng（底座对齐路面 oy）
        for (int z = oz - 54; z >= oz - 118; z -= 26) {
            place(level, "ludeng", ox + 20, oy, z);
        }
        for (int z = oz + 47; z <= oz + 99; z += 26) {
            place(level, "ludeng", ox + 20, oy, z);
        }
        for (int z = oz - 54; z <= oz + 47; z += 26) {
            place(level, "ludeng", ox - 49, oy, z);
            place(level, "ludeng", ox + 117, oy, z);
        }

    }

    // ===== 布局放置 =====

    /** 按布局条目放置：读结构 NBT 实际尺寸 → 由中心坐标换算锚点。 */
    private static void placeConfigured(ServerLevel level, Entry e, int ox, int oy, int oz) {
        CompoundTag tag = loadTemplate(level, e.name());
        if (tag == null) {
            return;
        }
        int[] size = readSize(tag);
        BlockPos center = new BlockPos(ox + e.x(), oy + e.y(), oz + e.z());
        BlockPos anchor = centerToAnchor(center, size, e.toRotation());
        GuimiMod.LOGGER.info("廷根市生成：{} 中心=({},{},{}) 尺寸=[{},{},{}] 锚点=({},{},{}) 旋转={}",
                e.name(), center.getX(), center.getY(), center.getZ(),
                size[0], size[1], size[2], anchor.getX(), anchor.getY(), anchor.getZ(), e.rotation());
        place(level, e.name(), tag, anchor.getX(), anchor.getY(), anchor.getZ(), e.toRotation(), true, true);
    }

    /**
     * 由中心坐标 + 结构原始尺寸换算放置锚点（结构西北角）。
     * <p>
     * 依据 Minecraft 结构旋转变换：CCW90 时 (x,z)→(z,-x)，CW90 时 (x,z)→(-z,x)，
     * CW180 时 (x,z)→(-x,-z)，NONE 保持原样。每个旋转下结构占据的范围不同，
     * 锚点换算公式也随之不同。
     */
    private static BlockPos centerToAnchor(BlockPos center, int[] size, Rotation rotation) {
        int sx = size[0];
        int sy = size[1];
        int sz = size[2];
        int ax;
        int az;
        switch (rotation) {
            case NONE -> {
                ax = center.getX() - sx / 2;
                az = center.getZ() - sz / 2;
            }
            case CLOCKWISE_90 -> {
                ax = center.getX() + sz / 2;
                az = center.getZ() - sx / 2;
            }
            case COUNTERCLOCKWISE_90 -> {
                ax = center.getX() - sz / 2;
                az = center.getZ() + sx / 2;
            }
            case CLOCKWISE_180 -> {
                ax = center.getX() + sx / 2;
                az = center.getZ() + sz / 2;
            }
            default -> {
                ax = center.getX();
                az = center.getZ();
            }
        }
        return new BlockPos(ax, center.getY() - sy / 2, az);
    }

    /** 计算结构在世界中的包围盒 [min, max]（锚点 + 旋转后的实际范围）。 */
    private static BlockPos[] structureBounds(BlockPos anchor, int[] size, Rotation rotation) {
        int sx = size[0];
        int sy = size[1];
        int sz = size[2];
        int x0, x1, z0, z1;
        switch (rotation) {
            case CLOCKWISE_90 -> {
                x0 = -(sz - 1);
                x1 = 0;
                z0 = 0;
                z1 = sx - 1;
            }
            case COUNTERCLOCKWISE_90 -> {
                x0 = 0;
                x1 = sz - 1;
                z0 = -(sx - 1);
                z1 = 0;
            }
            case CLOCKWISE_180 -> {
                x0 = -(sx - 1);
                x1 = 0;
                z0 = -(sz - 1);
                z1 = 0;
            }
            default -> {
                x0 = 0;
                x1 = sx - 1;
                z0 = 0;
                z1 = sz - 1;
            }
        }
        return new BlockPos[]{
                new BlockPos(anchor.getX() + x0, anchor.getY(), anchor.getZ() + z0),
                new BlockPos(anchor.getX() + x1, anchor.getY() + sy - 1, anchor.getZ() + z1)};
    }

    /** 清空结构占据区域为空气：无视地形，删除范围内的树木 / 水面 / 原生建筑等。 */
    private static void clearRegion(ServerLevel level, BlockPos min, BlockPos max) {
        BlockState air = Blocks.AIR.defaultBlockState();
        int count = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(Blocks.AIR)) {
                        count++;
                    }
                    level.setBlock(pos, air, Block.UPDATE_NONE);
                    level.getLightEngine().checkBlock(pos);
                }
            }
        }
        GuimiMod.LOGGER.info("廷根市生成：已清空占地区域（{}~{} 到 {}~{}，{}~{} 高度），清理 {} 个非空气方块",
                min.getX(), max.getX(), min.getZ(), max.getZ(), min.getY(), max.getY(), count);
    }

    /** 从结构 NBT 读取原始尺寸 [X, Y, Z]。兼容标准 IntArray 与 List<Int> 两种存储。 */
    private static int[] readSize(CompoundTag tag) {
        int[] size = new int[3];
        if (tag.contains("size", Tag.TAG_INT_ARRAY)) {
            int[] arr = tag.getIntArray("size");
            for (int i = 0; i < size.length && i < arr.length; i++) {
                size[i] = arr[i];
            }
        } else if (tag.contains("size", Tag.TAG_LIST)) {
            ListTag list = tag.getList("size", Tag.TAG_INT);
            for (int i = 0; i < size.length && i < list.size(); i++) {
                size[i] = list.getInt(i);
            }
        }
        return size;
    }

    /** 结构文件里当前版本（1.21.1）不存在的方块 → 替换表（例如高版本才加入的方块）。 */
    private static final Map<String, ResourceLocation> BLOCK_REMAPS = Map.of(
            "minecraft:pale_oak_wood", ResourceLocation.withDefaultNamespace("birch_wood"),
            "minecraft:creaking_heart", ResourceLocation.withDefaultNamespace("dark_oak_wood"));

    /** 解析结构 palette 中的方块 id；不存在时按替换表映射，仍找不到则回退石头并告警。 */
    private static Block resolveBlock(HolderGetter<Block> getter, ResourceLocation id) {
        ResourceLocation actual = id;
        if (id != null && getter.get(ResourceKey.create(Registries.BLOCK, id)).isEmpty()) {
            actual = BLOCK_REMAPS.getOrDefault(id.toString(),
                    ResourceLocation.withDefaultNamespace("stone"));
            GuimiMod.LOGGER.warn("廷根市生成：方块 {} 在当前版本不存在，已替换为 {}", id, actual);
        }
        return getter.getOrThrow(ResourceKey.create(Registries.BLOCK, actual)).value();
    }

    /** 锚点放置（无尺寸需求，马路 / 调试用）。 */
    private static void place(ServerLevel level, String name, int x, int y, int z) {
        place(level, name, x, y, z, Rotation.NONE);
    }

    private static void place(ServerLevel level, String name, int x, int y, int z, Rotation rotation) {
        place(level, name, x, y, z, rotation, false);
    }

    private static void place(ServerLevel level, String name, int x, int y, int z, Rotation rotation, boolean autoLight) {
        CompoundTag tag = loadTemplate(level, name);
        if (tag != null) {
            place(level, name, tag, x, y, z, rotation, autoLight, true);
        }
    }

    private static void place(ServerLevel level, String name, CompoundTag tag, int x, int y, int z, Rotation rotation) {
        place(level, name, tag, x, y, z, rotation, false, false);
    }

    private static void place(ServerLevel level, String name, CompoundTag tag, int x, int y, int z,
                              Rotation rotation, boolean autoLight, boolean clearTerrain) {
        GuimiMod.LOGGER.debug("廷根市生成：放置 {} 于 ({}, {}, {}) 旋转 {}", name, x, y, z, rotation);
        HolderGetter<Block> blockGetter = level.registryAccess().lookupOrThrow(Registries.BLOCK);

        // 无视地形：先清空建筑占据区域，避免原生地形 / 建筑 / 树木 / 水等残留穿透建筑
        if (clearTerrain) {
            int[] size = readSize(tag);
            BlockPos[] bounds = structureBounds(new BlockPos(x, y, z), size, rotation);
            clearRegion(level, bounds[0], bounds[1]);
        }

        // 解析 palette 得到 BlockState 数组
        ListTag paletteTag = tag.getList("palette", 10);
        BlockState[] states = new BlockState[paletteTag.size()];
        for (int i = 0; i < states.length; i++) {
            CompoundTag p = paletteTag.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(p.getString("Name"));
            Block block = resolveBlock(blockGetter, id);
            BlockState st = block.defaultBlockState();
            if (p.contains("Properties", 10)) {
                CompoundTag props = p.getCompound("Properties");
                for (String key : props.getAllKeys()) {
                    Property<?> prop = block.getStateDefinition().getProperty(key);
                    if (prop != null) {
                        st = applyProperty(st, prop, props.getString(key));
                    }
                }
            }
            states[i] = stabilize(st);
        }

        // 收集方块并按 Y 从下到上排序放置（flags=0 不触发任何更新），
        // 保证重力方块下方先有支撑，避免混凝土粉末/沙子等被随机 tick 拉下掉落
        List<PlacePos> list = new ArrayList<>();
        ListTag blocksTag = tag.getList("blocks", 10);
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag b = blocksTag.getCompound(i);
            int[] pos = readPos(b);
            if (pos == null) {
                continue;
            }
            int si = b.getInt("state");
            if (si < 0 || si >= states.length) {
                continue;
            }
            BlockState st = states[si];
            if (st.getBlock() == Blocks.STRUCTURE_BLOCK) {
                continue;
            }
            list.add(new PlacePos(new BlockPos(pos[0], pos[1], pos[2]), st));
        }
        list.sort(Comparator.comparingInt(p -> p.pos.getY()));
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (PlacePos p : list) {
            BlockPos transformed = StructureTemplate.transform(p.pos, Mirror.NONE, rotation, BlockPos.ZERO);
            BlockState st = p.state.mirror(Mirror.NONE).rotate(rotation);
            BlockPos wp = transformed.offset(x, y, z);
            level.setBlock(wp, st, Block.UPDATE_NONE);
            // setBlock 不会触发光照重算（checkBlock 不在此构建中被调用），
            // 显式标记该位置光照失效，否则建筑内部会保持旧光照而一片漆黑、生成怪物
            level.getLightEngine().checkBlock(wp);
            minX = Math.min(minX, wp.getX());
            minY = Math.min(minY, wp.getY());
            minZ = Math.min(minZ, wp.getZ());
            maxX = Math.max(maxX, wp.getX());
            maxY = Math.max(maxY, wp.getY());
            maxZ = Math.max(maxZ, wp.getZ());
        }
        if (autoLight) {
            autoLight(level, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        }
    }

    /** 自动补光方块：隐形光源方块（亮度 15），放在地板上完全隐形，不影响外观。 */
    private static final BlockState AUTO_LIGHT_STATE =
            Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);

    /**
     * 在结构包围盒内按网格补光，防止内部黑暗刷怪。
     * <p>
     * 对每个网格列先判断是否穿过建筑实体（过滤包围盒内但建筑外的空隙），
     * 再自顶向下找到每层的“地板”（空气上紧贴实体方块）位置放一盏 glowstone，
     * 并保证同一列上下间隔 ≥8 格。放完后同样显式触发光照重算。
     */
    private static void autoLight(ServerLevel level, BlockPos min, BlockPos max) {
        final int spacing = 6;
        final int margin = 3;
        int count = 0;
        for (int x = min.getX() + margin; x <= max.getX() - margin; x += spacing) {
            for (int z = min.getZ() + margin; z <= max.getZ() - margin; z += spacing) {
                boolean hasBlocks = false;
                for (int y = min.getY(); y <= max.getY(); y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        hasBlocks = true;
                        break;
                    }
                }
                if (!hasBlocks) {
                    continue;
                }
                for (int y = max.getY() - 1; y >= min.getY() + 1; ) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).isAir()
                            && !level.getBlockState(pos.below()).isAir()) {
                        level.setBlock(pos, AUTO_LIGHT_STATE, Block.UPDATE_NONE);
                        level.getLightEngine().checkBlock(pos);
                        count++;
                        y -= 8;
                    } else {
                        y--;
                    }
                }
            }
        }
        if (count > 0) {
            GuimiMod.LOGGER.info("廷根市生成：自动补光 {} 盏隐形光源（{}~{} 到 {}~{}）",
                    count, min.getX() + margin, max.getX() - margin, min.getZ() + margin, max.getZ() - margin);
        }
    }

    private static BlockState stabilize(BlockState state) {
        Block stable = GRAVITY_REPLACEMENTS.get(state.getBlock());
        return stable != null ? stable.defaultBlockState() : state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value).map(v -> state.setValue(prop, v)).orElse(state);
    }

    private static int[] readPos(CompoundTag block) {
        if (block.contains("pos", 11)) {
            return block.getIntArray("pos");
        }
        if (block.contains("pos", 9)) {
            ListTag list = block.getList("pos", 3);
            int[] pos = new int[list.size()];
            for (int k = 0; k < pos.length; k++) {
                pos[k] = list.getInt(k);
            }
            return pos;
        }
        return null;
    }

    /** 世界文件夹内的结构目录（<world>/guimi_structures/），合并产物与布局覆盖都放这里。 */
    private static Path worldStructuresDir(ServerLevel level) {
        return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("guimi_structures");
    }

    private static CompoundTag loadTemplate(ServerLevel level, String name) {
        // 优先世界结构（合并命令产物）：<world>/guimi_structures/<name>.nbt
        Path worldFile = worldStructuresDir(level).resolve(name + ".nbt");
        if (Files.exists(worldFile)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(worldFile, NbtAccounter.unlimitedHeap());
                GuimiMod.LOGGER.info("廷根市生成：已加载世界结构 {}（{} 方块）", name, tag.getList("blocks", 10).size());
                return tag;
            } catch (Exception e) {
                GuimiMod.LOGGER.warn("廷根市生成：读取世界结构 {} 失败，回退数据包", name, e);
            }
        }
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("guimi_mod", "structures/" + name + ".nbt");
        Optional<Resource> resource = level.getServer().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            GuimiMod.LOGGER.warn("廷根市生成：资源 {} 不存在（数据包未提供该结构文件）", file);
            return null;
        }
        try (InputStream in = resource.get().open()) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            GuimiMod.LOGGER.info("廷根市生成：已加载结构 {}（{} 方块）", name, tag.getList("blocks", 10).size());
            return tag;
        } catch (Exception e) {
            GuimiMod.LOGGER.warn("廷根市生成：解析结构 {} 失败", file, e);
            return null;
        }
    }

    // ===== 合并工具：把当前布局的多个结构合并成单个 church_all.nbt =====

    private record SourceBlock(BlockPos pos, BlockState state, CompoundTag palette) {
    }

    /**
     * 按当前布局把所有结构合并成单个 {@code church_all.nbt}，写入世界文件夹
     * {@code <world>/guimi_structures/}，并生成同名目录下的 {@code city_layout.json} 覆盖
     * （只含单个 church_all 条目）。合并后的结构用 {@code rotation=none} 放置即可复现原布局，
     * 不受结构方块 32×32×32 上限影响。
     *
     * @return 合并的方块总数；失败返回 -1
     */
    public static int mergeToSingle(ServerLevel level) {
        LAYOUT = CityLayout.load(level.getServer().getResourceManager(), level);

        List<SourceBlock> all = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int structureCount = 0;
        HolderGetter<Block> getter = level.registryAccess().lookupOrThrow(Registries.BLOCK);

        for (Entry e : LAYOUT.entries()) {
            CompoundTag tag = loadTemplate(level, e.name());
            if (tag == null) {
                continue;
            }
            structureCount++;
            int[] size = readSize(tag);
            Rotation rot = e.toRotation();
            BlockPos anchor = centerToAnchor(new BlockPos(e.x(), e.y(), e.z()), size, rot);

            // 解析 palette 得到 BlockState 数组
            ListTag paletteTag = tag.getList("palette", 10);
            BlockState[] states = new BlockState[paletteTag.size()];
            for (int i = 0; i < states.length; i++) {
                CompoundTag p = paletteTag.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(p.getString("Name"));
                Block block = resolveBlock(getter, id);
                BlockState st = block.defaultBlockState();
                if (p.contains("Properties", 10)) {
                    CompoundTag props = p.getCompound("Properties");
                    for (String key : props.getAllKeys()) {
                        Property<?> prop = block.getStateDefinition().getProperty(key);
                        if (prop != null) {
                            st = applyProperty(st, prop, props.getString(key));
                        }
                    }
                }
                states[i] = st;
            }

            ListTag blocksTag = tag.getList("blocks", 10);
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag b = blocksTag.getCompound(i);
                int[] pos = readPos(b);
                if (pos == null) {
                    continue;
                }
                int si = b.getInt("state");
                if (si < 0 || si >= states.length) {
                    continue;
                }
                BlockState st = states[si];
                if (st.getBlock() == Blocks.STRUCTURE_BLOCK) {
                    continue;
                }
                BlockPos tp = StructureTemplate.transform(
                        new BlockPos(pos[0], pos[1], pos[2]), Mirror.NONE, rot, BlockPos.ZERO)
                        .offset(anchor.getX(), anchor.getY(), anchor.getZ());
                BlockState rs = st.mirror(Mirror.NONE).rotate(rot);
                minX = Math.min(minX, tp.getX());
                minY = Math.min(minY, tp.getY());
                minZ = Math.min(minZ, tp.getZ());
                maxX = Math.max(maxX, tp.getX());
                maxY = Math.max(maxY, tp.getY());
                maxZ = Math.max(maxZ, tp.getZ());
                all.add(new SourceBlock(tp, rs, paletteTag.getCompound(si).copy()));
            }
        }

        int w = maxX - minX + 1;
        int h = maxY - minY + 1;
        int d = maxZ - minZ + 1;

        CompoundTag out = new CompoundTag();
        out.putIntArray("size", new int[]{w, h, d});
        ListTag outPalette = new ListTag();
        ListTag outBlocks = new ListTag();
        Map<BlockState, Integer> stateIdx = new HashMap<>();
        for (SourceBlock sb : all) {
            Integer idx = stateIdx.get(sb.state);
            if (idx == null) {
                idx = outPalette.size();
                stateIdx.put(sb.state, idx);
                outPalette.add(sb.palette);
            }
            CompoundTag block = new CompoundTag();
            block.putIntArray("pos", new int[]{
                    sb.pos.getX() - minX, sb.pos.getY() - minY, sb.pos.getZ() - minZ});
            block.putInt("state", idx);
            outBlocks.add(block);
        }
        out.put("palette", outPalette);
        out.put("blocks", outBlocks);

        try {
            Path dir = worldStructuresDir(level);
            Files.createDirectories(dir);
            NbtIo.writeCompressed(out, dir.resolve("church_all.nbt"));

            // 布局覆盖：只保留合并结构（rotation=none，中心=包围盒中心）
            int cx = minX + w / 2;
            int cy = minY + h / 2;
            int cz = minZ + d / 2;
            String json = "{\n  \"structures\": [\n"
                    + "    { \"name\": \"church_all\", \"x\": " + cx
                    + ", \"y\": " + cy + ", \"z\": " + cz + ", \"rotation\": \"none\" }\n"
                    + "  ]\n}\n";
            Files.writeString(dir.resolve("city_layout.json"), json, StandardCharsets.UTF_8);

            GuimiMod.LOGGER.info("廷根市生成：已合并 {} 个结构为 church_all.nbt（{}×{}×{}，{} 方块），"
                    + "中心偏移 ({},{},{})", structureCount, w, h, d, all.size(), cx, cy, cz);
            return all.size();
        } catch (Exception e) {
            GuimiMod.LOGGER.warn("廷根市生成：写入合并结构失败", e);
            return -1;
        }
    }

    // ===== 调试 / 验证工具（供命令调用） =====

    /** 上次放置的标记方块位置，供 {@link #clearMarkers(ServerLevel)} 清理。 */
    private static List<BlockPos> MARKERS = List.of();

    /** 结构规划信息（中心 / 锚点 / 尺寸），命令用于打印与放置标记。 */
    public record Planned(String name, BlockPos center, BlockPos anchor, int[] size) {
    }

    /** 城市原点（世界坐标）：出生点偏移 (-20, +46)，y 取出生点地表高度。 */
    public static BlockPos cityOrigin(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        int oy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
        return new BlockPos(spawn.getX() - 20, oy, spawn.getZ() + 46);
    }

    /** 计算当前布局在指定世界中的规划（不含马路）。 */
    public static List<Planned> plan(ServerLevel level) {
        BlockPos origin = cityOrigin(level);
        List<Planned> result = new ArrayList<>();
        for (Entry e : LAYOUT.entries()) {
            CompoundTag tag = loadTemplate(level, e.name());
            if (tag == null) {
                continue;
            }
            int[] size = readSize(tag);
            BlockPos center = new BlockPos(origin.getX() + e.x(), origin.getY() + e.y(), origin.getZ() + e.z());
            BlockPos anchor = centerToAnchor(center, size, e.toRotation());
            result.add(new Planned(e.name(), center, anchor, size));
        }
        return result;
    }

    /** 放置标记方块：中心=金块、锚点=青金石块。记录位置供 {@link #clearMarkers(ServerLevel)} 清理。 */
    public static int placeMarkers(ServerLevel level) {
        List<BlockPos> placed = new ArrayList<>();
        for (Planned p : plan(level)) {
            BlockPos topCenter = new BlockPos(p.center().getX(),
                    Math.max(p.center().getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.center().getX(), p.center().getZ())),
                    p.center().getZ());
            level.setBlock(topCenter.above(), Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
            placed.add(topCenter.above());
            level.setBlock(p.anchor(), Blocks.LAPIS_BLOCK.defaultBlockState(), Block.UPDATE_NONE);
            placed.add(p.anchor());
        }
        MARKERS = placed;
        return placed.size();
    }

    /** 清除上次放置的标记方块（恢复为空气）。 */
    public static void clearMarkers(ServerLevel level) {
        for (BlockPos pos : MARKERS) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
        MARKERS = List.of();
    }

    /** 标记城市为未生成：下次服务器启动（或重启世界）时会按当前布局重新生成。 */
    public static void resetGenerated(ServerLevel level) {
        LoenCityData data = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<LoenCityData>(LoenCityData::new, LoenCityData::load, null), SAVED_KEY);
        data.generated = false;
        data.setDirty();
    }

    /** 免重启调位置：重新加载布局（数据包或世界覆盖）并立即重放标记。返回标记方块数。 */
    public static int reloadAndMark(ServerLevel level) {
        LAYOUT = CityLayout.load(level.getServer().getResourceManager(), level);
        clearMarkers(level);
        return placeMarkers(level);
    }

    // ===== 手动调整结构位置（写入世界布局覆盖，免重启） =====

    /** 世界布局覆盖文件：<world>/guimi_structures/city_layout.json */
    private static Path worldLayoutFile(ServerLevel level) {
        return worldStructuresDir(level).resolve("city_layout.json");
    }

    /** 读取世界布局覆盖中的条目（未覆盖的返回空表）。 */
    private static Map<String, Entry> readWorldOverride(ServerLevel level) {
        Map<String, Entry> map = new HashMap<>();
        Path file = worldLayoutFile(level);
        if (!Files.exists(file)) {
            return map;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("structures")) {
                for (JsonElement el : root.getAsJsonArray("structures")) {
                    JsonObject o = el.getAsJsonObject();
                    String name = o.get("name").getAsString();
                    map.put(name, new Entry(name,
                            o.has("x") ? o.get("x").getAsInt() : 0,
                            o.has("y") ? o.get("y").getAsInt() : 0,
                            o.has("z") ? o.get("z").getAsInt() : 0,
                            o.has("rotation") ? o.get("rotation").getAsString() : "none"));
                }
            }
        } catch (Exception e) {
            GuimiMod.LOGGER.warn("廷根市生成：读取世界布局覆盖失败", e);
        }
        return map;
    }

    /** 把世界布局覆盖写回文件。 */
    private static void writeWorldOverride(ServerLevel level, Map<String, Entry> map) {
        try {
            Path dir = worldStructuresDir(level);
            Files.createDirectories(dir);
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Entry e : map.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", e.name());
                o.addProperty("x", e.x());
                o.addProperty("y", e.y());
                o.addProperty("z", e.z());
                o.addProperty("rotation", e.rotation());
                arr.add(o);
            }
            root.add("structures", arr);
            Files.writeString(worldLayoutFile(level),
                    new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            GuimiMod.LOGGER.warn("廷根市生成：写入世界布局覆盖失败", e);
        }
    }

    /** 生效本次调整：重载布局并重放标记。 */
    private static void applyOverride(ServerLevel level) {
        LAYOUT = CityLayout.load(level.getServer().getResourceManager(), level);
        clearMarkers(level);
        placeMarkers(level);
    }

    /**
     * 手动调整位置（以脚下为 x/z 中心，y 保持当前值）：把结构名写入世界布局覆盖。
     *
     * @return 1 成功；-1 结构不存在
     */
    public static int setHere(ServerLevel level, String name, BlockPos feet, String rotation) {
        Entry cur = LAYOUT.get(name);
        if (cur == null) {
            return -1;
        }
        BlockPos origin = cityOrigin(level);
        Map<String, Entry> map = readWorldOverride(level);
        map.put(name, new Entry(name, feet.getX() - origin.getX(), cur.y(), feet.getZ() - origin.getZ(),
                rotation != null ? rotation : cur.rotation()));
        writeWorldOverride(level, map);
        applyOverride(level);
        return 1;
    }

    /**
     * 手动调整位置（以世界坐标为结构中心）：把结构名写入世界布局覆盖。
     *
     * @return 1 成功；-1 结构不存在
     */
    public static int setPos(ServerLevel level, String name, BlockPos pos, String rotation) {
        Entry cur = LAYOUT.get(name);
        if (cur == null) {
            return -1;
        }
        BlockPos origin = cityOrigin(level);
        Map<String, Entry> map = readWorldOverride(level);
        map.put(name, new Entry(name, pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(),
                rotation != null ? rotation : cur.rotation()));
        writeWorldOverride(level, map);
        applyOverride(level);
        return 1;
    }

    private record PlacePos(BlockPos pos, BlockState state) {
    }

    private static class LoenCityData extends SavedData {
        boolean generated;

        public LoenCityData() {
        }

        public static LoenCityData load(CompoundTag tag, HolderLookup.Provider registries) {
            LoenCityData data = new LoenCityData();
            data.generated = tag.getBoolean("generated");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("generated", generated);
            return tag;
        }
    }

    // ===== 布局配置模型 =====

    /** 单个结构的布局条目：中心坐标偏移（相对城市原点）+ 旋转。 */
    public record Entry(String name, int x, int y, int z, String rotation) {

        /** 解析旋转字符串（none / cw90 / ccw90 / cw180），无法识别时回退 NONE。 */
        Rotation toRotation() {
            return switch (rotation) {
                case "cw90" -> Rotation.CLOCKWISE_90;
                case "ccw90" -> Rotation.COUNTERCLOCKWISE_90;
                case "cw180" -> Rotation.CLOCKWISE_180;
                default -> Rotation.NONE;
            };
        }
    }

    /** 布局配置容器：缺失的结构自动使用默认值。 */
    public static class CityLayout {

        private final Map<String, Entry> entries = new HashMap<>();

        public CityLayout() {
            entries.putAll(defaults());
        }

        /** 覆盖（或新增）一个结构的配置。 */
        public void put(Entry e) {
            entries.put(e.name(), e);
        }

        /** 全部条目（按默认顺序，未被覆盖的保持默认）。 */
        public List<Entry> entries() {
            List<Entry> list = new ArrayList<>();
            Map<String, Entry> ordered = new LinkedHashMap<>(defaults());
            ordered.putAll(entries);
            for (Entry e : ordered.values()) {
                list.add(e);
            }
            return list;
        }

        /** 读取一个结构的配置（缺失返回默认）。 */
        public Entry get(String name) {
            Entry e = entries.get(name);
            return e != null ? e : defaults().get(name);
        }

        /** 从数据包 city_layout.json 加载；优先读取世界文件夹覆盖（<world>/guimi_structures/city_layout.json）。 */
        public static CityLayout load(net.minecraft.server.packs.resources.ResourceManager manager, ServerLevel level) {
            CityLayout layout = new CityLayout();

            // 世界覆盖优先（合并命令产物）
            Path worldLayout = worldStructuresDir(level).resolve("city_layout.json");
            if (Files.exists(worldLayout)) {
                try (Reader reader = Files.newBufferedReader(worldLayout, StandardCharsets.UTF_8)) {
                    if (parseLayout(layout, reader)) {
                        GuimiMod.LOGGER.info("廷根市生成：已加载世界布局覆盖 {}", worldLayout);
                        return layout;
                    }
                } catch (Exception e) {
                    GuimiMod.LOGGER.warn("廷根市生成：解析世界布局覆盖 {} 失败，回退数据包", worldLayout, e);
                }
            }

            Optional<Resource> res = manager.getResource(
                    ResourceLocation.fromNamespaceAndPath("guimi_mod", "city_layout.json"));
            if (res.isEmpty()) {
                GuimiMod.LOGGER.info("廷根市生成：未找到 city_layout.json，使用内置默认布局");
                return layout;
            }
            try (Reader reader = res.get().openAsReader()) {
                if (parseLayout(layout, reader)) {
                    GuimiMod.LOGGER.info("廷根市生成：已从 city_layout.json 加载布局（{} 个结构）", layout.entries().size());
                }
            } catch (Exception e) {
                GuimiMod.LOGGER.warn("廷根市生成：解析 city_layout.json 失败，使用内置默认布局", e);
            }
            return layout;
        }

        private static boolean parseLayout(CityLayout layout, Reader reader) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("structures")) {
                JsonArray arr = root.getAsJsonArray("structures");
                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    String name = o.get("name").getAsString();
                    int x = o.has("x") ? o.get("x").getAsInt() : 0;
                    int y = o.has("y") ? o.get("y").getAsInt() : 0;
                    int z = o.has("z") ? o.get("z").getAsInt() : 0;
                    String rot = o.has("rotation") ? o.get("rotation").getAsString() : "none";
                    layout.put(new Entry(name, x, y, z, rot));
                }
                return true;
            }
            return false;
        }
    }
}
