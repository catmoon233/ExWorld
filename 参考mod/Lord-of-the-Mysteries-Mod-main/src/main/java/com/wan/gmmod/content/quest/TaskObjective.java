package com.wan.gmmod.content.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 任务目标：{@code type} 定义上报事件类型，{@code target} 为事件的具体键值
 * （实体 ID / 物品 ID / 能力 ID / 序列 ID / 结构 ID），{@code count} 为目标数量。
 * <p>
 * 进度存储在 {@code QUEST_PROGRESS} 附件中，键为 {@code taskId + ":" + 目标序号}。
 */
public record TaskObjective(String type, String target, int count) {

    public static final Codec<TaskObjective> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("type").forGetter(TaskObjective::type),
            Codec.STRING.fieldOf("target").forGetter(TaskObjective::target),
            Codec.INT.optionalFieldOf("count", 1).forGetter(TaskObjective::count)
    ).apply(inst, TaskObjective::new));

    /** 目标序号的进度键前缀。 */
    public static String progressKey(String taskId, int index) {
        return taskId + ":" + index;
    }
}
