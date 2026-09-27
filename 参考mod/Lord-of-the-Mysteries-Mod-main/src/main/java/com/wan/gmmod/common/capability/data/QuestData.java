package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务进度数据：
 * <ul>
 *   <li>{@code active}：进行中的任务 ID 列表（接取后加入，完成后移除）；</li>
 *   <li>{@code completed}：已完成的任务 ID 列表（用于前置条件判定与图鉴灰显）；</li>
 *   <li>{@code progress}：目标进度，键为 {@code taskId:index}，值为当前进度数；</li>
 *   <li>{@code tracked}：HUD 追踪的任务 ID（最多 3 个）。</li>
 * </ul>
 * 服务端权威写入，同步到客户端供任务书 / HUD 显示。不可变：修改返回新实例以触发附件同步。
 */
public record QuestData(List<String> active, List<String> completed,
                        Map<String, Integer> progress, List<String> tracked) {

    public static final Codec<QuestData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("active", List.of()).forGetter(QuestData::active),
            Codec.STRING.listOf().optionalFieldOf("completed", List.of()).forGetter(QuestData::completed),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("progress", Map.of())
                    .forGetter(QuestData::progress),
            Codec.STRING.listOf().optionalFieldOf("tracked", List.of()).forGetter(QuestData::tracked)
    ).apply(inst, QuestData::new));

    public static final StreamCodec<io.netty.buffer.ByteBuf, QuestData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), QuestData::active,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), QuestData::completed,
                    ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), QuestData::progress,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), QuestData::tracked,
                    QuestData::new);

    public static QuestData empty() {
        return new QuestData(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    public boolean hasActive(String taskId) {
        return active.contains(taskId);
    }

    public boolean hasActive() {
        return !active.isEmpty();
    }

    public boolean hasCompleted(String taskId) {
        return completed.contains(taskId);
    }

    public boolean hasCompleted() {
        return !completed.isEmpty();
    }

    /** 接取任务：写入 active，若同时满足条件则移除 tracked 中已废弃的条目。 */
    public QuestData withActive(String taskId) {
        List<String> a = new ArrayList<>(active);
        if (!a.contains(taskId)) {
            a.add(taskId);
        }
        return new QuestData(a, completed, progress, tracked);
    }

    /** 放弃任务：从 active 移除，并清理其进度与追踪。 */
    public QuestData withoutActive(String taskId, int objectiveCount) {
        List<String> a = new ArrayList<>(active);
        a.remove(taskId);
        Map<String, Integer> p = new HashMap<>(progress);
        for (int i = 0; i < objectiveCount; i++) {
            p.remove(taskId + ":" + i);
        }
        List<String> t = new ArrayList<>(tracked);
        t.remove(taskId);
        return new QuestData(a, completed, p, t);
    }

    /** 完成：从 active 移除，写入 completed，清理进度与追踪。 */
    public QuestData withCompleted(String taskId, int objectiveCount) {
        List<String> a = new ArrayList<>(active);
        a.remove(taskId);
        List<String> c = new ArrayList<>(completed);
        if (!c.contains(taskId)) {
            c.add(taskId);
        }
        Map<String, Integer> p = new HashMap<>(progress);
        for (int i = 0; i < objectiveCount; i++) {
            p.remove(taskId + ":" + i);
        }
        List<String> t = new ArrayList<>(tracked);
        t.remove(taskId);
        return new QuestData(a, c, p, t);
    }

    /** 记录一条目标进度（只增不减）。 */
    public QuestData withProgress(String key, int value) {
        Map<String, Integer> p = new HashMap<>(progress);
        int old = p.getOrDefault(key, 0);
        if (value > old) {
            p.put(key, value);
        }
        return new QuestData(active, completed, p, tracked);
    }

    /** 追踪 / 取消追踪任务。最多 3 个。 */
    public QuestData withTracked(String taskId) {
        List<String> t = new ArrayList<>(tracked);
        if (t.contains(taskId)) {
            t.remove(taskId);
        } else {
            while (t.size() >= 3) {
                t.remove(0);
            }
            t.add(taskId);
        }
        return new QuestData(active, completed, progress, t);
    }

    /** 取目标进度，无记录返回 0。 */
    public int progressOf(String key) {
        Integer v = progress.get(key);
        return v == null ? 0 : v;
    }
}
