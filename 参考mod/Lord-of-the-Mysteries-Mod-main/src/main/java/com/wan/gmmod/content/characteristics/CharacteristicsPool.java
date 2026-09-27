package com.wan.gmmod.content.characteristics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wan.gmmod.content.sequences.Sequences;

import java.util.HashMap;
import java.util.Map;

/**
 * 世界级非凡特性全局池（守恒定律的核心数据）。
 * <p>
 * 记录当前世界中每种途径、每个序列等级的特性总数量（含玩家身上、掉落物、生物携带）。
 * 以扁平 {@code Map<String, Integer>} 存储，键为 {@code pathwayKey + "_" + level}，
 * 便于 Codec 序列化（避免整型键的 JSON 映射问题）。
 * <p>
 * 作为 {@link net.minecraft.world.level.Level} 级别的 Attachment 持久化，服务端权威、无需同步。
 * {@code initialized} 标记世界是否已完成首次特性发放，避免重复初始化。
 */
public class CharacteristicsPool {
    public static final Codec<CharacteristicsPool> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("counts").forGetter(p -> p.counts),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("pending", new HashMap<>()).forGetter(p -> p.pending),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(p -> p.initialized)
    ).apply(inst, CharacteristicsPool::new));

    private final Map<String, Integer> counts;
    /** 未分配（尚未物理散布到世界宝箱 / 生物中）的特性数量，供守恒控制掉落。 */
    private final Map<String, Integer> pending;
    private boolean initialized;

    public CharacteristicsPool() {
        this(new HashMap<>(), new HashMap<>(), false);
    }

    public CharacteristicsPool(Map<String, Integer> counts, Map<String, Integer> pending, boolean initialized) {
        this.counts = new HashMap<>(counts);
        this.pending = new HashMap<>(pending);
        this.initialized = initialized;
    }

    private static String key(Sequences.Pathway pathway, int level) {
        return pathway.getKey() + "_" + level;
    }

    /** 查询指定途径 / 等级的当前特性总量。 */
    public int get(Sequences.Pathway pathway, int level) {
        return counts.getOrDefault(key(pathway, level), 0);
    }

    /** 设置指定途径 / 等级的特性总量（下限 0）。 */
    public void set(Sequences.Pathway pathway, int level, int value) {
        counts.put(key(pathway, level), Math.max(0, value));
    }

    /** 在指定途径 / 等级上增减特性数量，返回调整后的实际值。 */
    public int add(Sequences.Pathway pathway, int level, int delta) {
        int next = Math.max(0, get(pathway, level) + delta);
        counts.put(key(pathway, level), next);
        return next;
    }

    /** 是否已完成世界首次特性发放。 */
    public boolean isInitialized() {
        return initialized;
    }

    /** 标记世界已完成首次特性发放。 */
    public void markInitialized() {
        this.initialized = true;
    }

    /** 全世界所有途径 / 等级的特性总量。 */
    public int total() {
        int sum = 0;
        for (int v : counts.values()) {
            sum += v;
        }
        return sum;
    }

    // ===== 未分配（pending）池：控制物理特性物品的世界发放，保证守恒 =====

    /** 查询指定途径 / 等级尚未物理散布的数量。 */
    public int getPending(Sequences.Pathway pathway, int level) {
        return pending.getOrDefault(key(pathway, level), 0);
    }

    /** 设置指定途径 / 等级的未分配数量（下限 0）。 */
    public void setPending(Sequences.Pathway pathway, int level, int value) {
        pending.put(key(pathway, level), Math.max(0, value));
    }

    /** 在未分配池上增减数量，返回调整后的实际值。 */
    public int addPending(Sequences.Pathway pathway, int level, int delta) {
        int next = Math.max(0, getPending(pathway, level) + delta);
        pending.put(key(pathway, level), next);
        return next;
    }

    /** 未分配池总量（仍待散布到世界的特性数）。 */
    public int totalPending() {
        int sum = 0;
        for (int v : pending.values()) {
            sum += v;
        }
        return sum;
    }
}
