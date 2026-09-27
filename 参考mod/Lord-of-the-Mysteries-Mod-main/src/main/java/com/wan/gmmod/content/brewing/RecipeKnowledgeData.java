package com.wan.gmmod.content.brewing;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家已研读的魔药配方集合（配方卷轴阅读后写入）。
 * <p>
 * 服务端权威：炼药锅合成前校验目标配方是否在列；死亡保留。
 * 不可变：修改返回新实例以触发附件自动同步。
 *
 * @param recipes 已掌握的配方 ID 列表（即魔药物品注册名 path，如 {@code "seer_potion"}）
 */
public record RecipeKnowledgeData(List<String> recipes) {

    public static final Codec<RecipeKnowledgeData> CODEC =
            Codec.STRING.listOf().xmap(RecipeKnowledgeData::new, RecipeKnowledgeData::recipes);

    public static final StreamCodec<io.netty.buffer.ByteBuf, RecipeKnowledgeData> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
                    .map(RecipeKnowledgeData::new, RecipeKnowledgeData::recipes);

    public static RecipeKnowledgeData empty() {
        return new RecipeKnowledgeData(new ArrayList<>());
    }

    /** 是否已掌握指定配方。 */
    public boolean contains(String recipeId) {
        return recipeId != null && recipes.contains(recipeId);
    }

    /** 返回掌握指定配方后的新实例；若已存在则返回自身。 */
    public RecipeKnowledgeData with(String recipeId) {
        if (contains(recipeId)) {
            return this;
        }
        List<String> copy = new ArrayList<>(recipes);
        copy.add(recipeId);
        return new RecipeKnowledgeData(copy);
    }
}
