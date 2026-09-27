package com.wan.gmmod.content.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 任务奖励。{@code type} 取值：
 * <ul>
 *   <li>{@code item}：物品奖励，{@code item} 为物品 ID，{@code amount} 为数量；</li>
 *   <li>{@code acting}：扮演进度奖励，{@code amount} 为进度值；</li>
 *   <li>{@code spirituality}：灵性奖励，{@code amount} 为灵性值。</li>
 * </ul>
 */
public record TaskReward(String type, String item, int amount, String effect) {

    public static final Codec<TaskReward> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("type").forGetter(TaskReward::type),
            Codec.STRING.optionalFieldOf("item", "").forGetter(TaskReward::item),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(TaskReward::amount),
            Codec.STRING.optionalFieldOf("effect", "").forGetter(TaskReward::effect)
    ).apply(inst, TaskReward::new));
}
