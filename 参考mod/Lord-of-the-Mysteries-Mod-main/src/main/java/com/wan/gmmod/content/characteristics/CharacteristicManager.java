package com.wan.gmmod.content.characteristics;

import com.wan.gmmod.Config;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 非凡特性全局管理器：全局池读写、生物「特性承载」标记、掉落生成与守恒转移的统一入口。
 * <p>
 * 全局池以 {@link net.minecraft.world.level.Level} 级 Attachment 存储（{@link ModAttachments#CHARACTERISTICS_POOL}），
 * 仅在服务端存在。生物是否承载特性通过其持久化 NBT（{@link Entity#getPersistentData()}）标记，
 * 避免为每个实体新增可空 Attachment。
 */
public final class CharacteristicManager {
    /** 生物持久化 NBT 中标记「承载特性」的键。 */
    private static final String CARRIER_TAG = "guimi_characteristic";
    private static final String TAG_PATHWAY = "pathway";
    private static final String TAG_LEVEL = "level";

    private CharacteristicManager() {
    }

    // ===== 全局池访问 =====

    /**
     * 解析全局池所在的世界。
     * <p>
     * 池按「全局」语义存储在主世界（Overworld）上，任何维度的调用都会路由到主世界，
     * 保证跨维度共享同一份守恒数据。
     */
    private static Level poolLevel(Level level) {
        return level.getServer() != null ? level.getServer().overworld() : level;
    }

    /** 获取世界全局池（服务端权威，始终取主世界实例）。 */
    public static CharacteristicsPool pool(Level level) {
        return poolLevel(level).getData(ModAttachments.CHARACTERISTICS_POOL);
    }

    /** 写回全局池以标记存档为脏（改动可持久化）。 */
    public static void save(Level level, CharacteristicsPool pool) {
        poolLevel(level).setData(ModAttachments.CHARACTERISTICS_POOL, pool);
    }

    /**
     * 世界首次加载时，按配置初始化全局池各途径各等级的特性总量。幂等：已初始化则跳过。
     */
    public static void initWorldPool(ServerLevel overworld) {
        CharacteristicsPool pool = overworld.getData(ModAttachments.CHARACTERISTICS_POOL);
        if (pool.isInitialized()) {
            return;
        }
        List<? extends Integer> totals = Config.CHARACTERISTIC_INITIAL_TOTALS.get();
        for (Sequences.Pathway pathway : Sequences.Pathway.values()) {
            for (int lvl = 0; lvl <= Sequences.MAX_LEVEL; lvl++) {
                int total = lvl < totals.size() ? totals.get(lvl) : 0;
                pool.set(pathway, lvl, total);
                // 初始时全部特性均「未分配」，等待通过宝箱战利品表物理散布到世界
                pool.setPending(pathway, lvl, total);
            }
        }
        pool.markInitialized();
        overworld.setData(ModAttachments.CHARACTERISTICS_POOL, pool);
    }

    /**
     * 守恒控制下的宝箱发放：从未分配池中按加权随机取一份特性并生成对应物品栈。
     * <p>
     * 权重取该 (途径, 等级) 的剩余未分配量，低序列（数量多）更常见；未分配池耗尽则返回 {@code null}，
     * 从而保证世界中物理散布的特性总量不超过配置初始总量（总量守恒）。
     *
     * @return 一份特性物品栈；若无可分配特性则为 {@code null}
     */
    public static ItemStack drawFromPending(Level level, net.minecraft.util.RandomSource random) {
        if (level.isClientSide()) {
            return null;
        }
        CharacteristicsPool pool = pool(level);
        int totalPending = pool.totalPending();
        if (totalPending <= 0) {
            return null;
        }
        int roll = random.nextInt(totalPending);
        for (Sequences.Pathway pathway : Sequences.Pathway.values()) {
            for (int lvl = 0; lvl <= Sequences.MAX_LEVEL; lvl++) {
                int p = pool.getPending(pathway, lvl);
                if (p <= 0) {
                    continue;
                }
                roll -= p;
                if (roll < 0) {
                    pool.addPending(pathway, lvl, -1);
                    save(level, pool);
                    return CharacteristicItem.create(pathway, lvl);
                }
            }
        }
        return null;
    }

    /** 在全局池中增减某途径某等级的特性数量并持久化。 */
    public static void addToPool(Level level, Sequences.Pathway pathway, int seqLevel, int delta) {
        if (level.isClientSide() || pathway == null) {
            return;
        }
        CharacteristicsPool pool = pool(level);
        pool.add(pathway, seqLevel, delta);
        save(level, pool);
    }

    /**
     * 守恒转移：低序列特性 → 高序列特性（晋升时调用）。
     * 全局池低序列 -1、高序列 +1，总量守恒。
     */
    public static void transferUp(Level level, Sequences.Pathway pathway, int fromLevel, int toLevel) {
        if (level.isClientSide() || pathway == null) {
            return;
        }
        CharacteristicsPool pool = pool(level);
        pool.add(pathway, fromLevel, -1);
        pool.add(pathway, toLevel, 1);
        save(level, pool);
    }

    /**
     * 守恒返还：高序列特性 → 低序列特性（降级 / 死亡时调用）。
     * 全局池高序列 -1、低序列 +1。
     */
    public static void transferDown(Level level, Sequences.Pathway pathway, int fromLevel, int toLevel) {
        if (level.isClientSide() || pathway == null) {
            return;
        }
        CharacteristicsPool pool = pool(level);
        pool.add(pathway, fromLevel, -1);
        pool.add(pathway, toLevel, 1);
        save(level, pool);
    }

    // ===== 生物「特性承载」标记 =====

    /** 标记生物承载指定途径 / 等级的特性（死亡时会掉落对应特性物品）。 */
    public static void markCarrier(Entity entity, Sequences.Pathway pathway, int seqLevel) {
        if (pathway == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_PATHWAY, pathway.getKey());
        tag.putInt(TAG_LEVEL, seqLevel);
        entity.getPersistentData().put(CARRIER_TAG, tag);
    }

    /** 读取生物承载的特性数据，未承载返回 {@code null}。 */
    public static CharacteristicData getCarrier(Entity entity) {
        if (!entity.getPersistentData().contains(CARRIER_TAG)) {
            return null;
        }
        CompoundTag tag = entity.getPersistentData().getCompound(CARRIER_TAG);
        Sequences.Pathway pathway = Sequences.fromKey(tag.getString(TAG_PATHWAY));
        if (pathway == null) {
            return null;
        }
        return new CharacteristicData(pathway.getKey(), tag.getInt(TAG_LEVEL));
    }

    /** 生物是否承载特性。 */
    public static boolean isCarrier(Entity entity) {
        return entity.getPersistentData().contains(CARRIER_TAG);
    }

    // ===== 掉落 / 生成（含聚合定律的吸引效果）=====

    /**
     * 在指定坐标生成一份特性物品；若开启聚合定律，则赋予其飞向附近同 / 相近途径玩家的初速度。
     */
    public static void spawnCharacteristic(Level level, Vec3 pos, Sequences.Pathway pathway, int seqLevel) {
        if (level.isClientSide() || pathway == null) {
            return;
        }
        ItemStack stack = CharacteristicItem.create(pathway, seqLevel);
        ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
        drop.setPickUpDelay(20);

        if (Config.CHARACTERISTIC_DROP_ATTRACTION.get()) {
            attractToward(level, drop, pathway, pos);
        }
        level.addFreshEntity(drop);
    }

    /** 让掉落物飞向搜索半径内最近的同 / 相近途径玩家（聚合定律 · 掉落吸引）。 */
    private static void attractToward(Level level, ItemEntity drop, Sequences.Pathway pathway, Vec3 pos) {
        double range = effectiveAggregationRange(level, pos, Config.CHARACTERISTIC_ATTRACTION_RANGE.get());
        net.minecraft.world.entity.player.Player target = null;
        double best = Double.MAX_VALUE;
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            Sequences.Pathway pp = Sequences.fromKey(player.getData(ModAttachments.PATHWAY));
            if (pp == null || !pathway.isProximate(pp)) {
                continue;
            }
            double d = player.distanceToSqr(pos.x, pos.y, pos.z);
            if (d < best && d <= range * range) {
                best = d;
                target = player;
            }
        }
        if (target != null) {
            Vec3 dir = new Vec3(target.getX() - pos.x, target.getY() + 0.5 - pos.y, target.getZ() - pos.z).normalize();
            drop.setDeltaMovement(dir.scale(0.35));
        }
    }

    /** 查询搜索半径内承载 / 掉落的特性物品实体（供天使级放牧使用）。 */
    public static List<ItemEntity> nearbyCharacteristicItems(Level level, Vec3 center, double range) {
        return level.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(range),
                e -> CharacteristicItem.getData(e.getItem()) != null);
    }

    /**
     * 聚合定律 · 半径成长：搜索范围内特性的物理物品越多，聚合半径越大，
     * 更容易吸引同 / 相近途径的生物与玩家。
     * <p>
     * 基数半径 = {@code baseRange}，每多一份特性 / 封印物物品，在基数上按
     * 配置倍率增长，最大不超过基数 × {@code aggregationMaxMultiplier}。
     */
    public static double effectiveAggregationRange(Level level, Vec3 center, double baseRange) {
        int nearby = 0;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(baseRange * 2))) {
            if (isIndestructibleItem(ie.getItem())) {
                nearby++;
            }
        }
        double grown = baseRange * (1 + nearby * Config.AGGREGATION_GROWTH_PER_ITEM.get());
        return Math.min(grown, baseRange * Config.AGGREGATION_MAX_MULTIPLIER.get());
    }

    /** 是否为不灭物品：非凡特性或封印物。 */
    public static boolean isIndestructibleItem(ItemStack stack) {
        return CharacteristicItem.getData(stack) != null
                || com.wan.gmmod.common.item.SealedArtifactItem.getData(stack) != null
                || com.wan.gmmod.common.item.MagicArtifactItem.getData(stack) != null;
    }
}
