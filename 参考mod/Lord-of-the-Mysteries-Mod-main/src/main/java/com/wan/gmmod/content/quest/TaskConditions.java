package com.wan.gmmod.content.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 任务解锁条件。所有字段均为可选项，未填写的条件不参与校验。
 * <ul>
 *   <li>{@code requiredQuest}：前置任务 ID（必须已完成）；</li>
 *   <li>{@code pathway}：途径 key（如 {@code fool}），限制仅该途径可接；</li>
 *   <li>{@code minSequence} / {@code maxSequence}：序列等级范围；</li>
 *   <li>{@code holdItem}：要求背包持有指定物品 ID。</li>
 * </ul>
 */
public record TaskConditions(String requiredQuest, String pathway,
                             int minSequence, int maxSequence, String holdItem) {

    public static final Codec<TaskConditions> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("requiredQuest", "").forGetter(TaskConditions::requiredQuest),
            Codec.STRING.optionalFieldOf("pathway", "").forGetter(TaskConditions::pathway),
            Codec.INT.optionalFieldOf("minSequence", 0).forGetter(TaskConditions::minSequence),
            Codec.INT.optionalFieldOf("maxSequence", 9).forGetter(TaskConditions::maxSequence),
            Codec.STRING.optionalFieldOf("holdItem", "").forGetter(TaskConditions::holdItem)
    ).apply(inst, TaskConditions::new));

    public static TaskConditions none() {
        return new TaskConditions("", "", 0, 9, "");
    }
}
