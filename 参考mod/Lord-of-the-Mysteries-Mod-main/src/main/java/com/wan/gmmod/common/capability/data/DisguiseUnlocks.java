package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 已解锁的人形怪物外观集合（怪物图鉴）。
 * <p>
 * 通过击杀、观察（潜行 + 右键）或初始赠送写入，存储各怪物实体类型 ID 的字符串。
 * 不可变：修改返回新实例以触发附件自动同步。
 *
 * @param ids 已解锁怪物类型 ID 字符串集合（保持插入顺序）
 */
public record DisguiseUnlocks(List<String> ids) {

    public static final Codec<DisguiseUnlocks> CODEC =
            Codec.STRING.listOf().xmap(DisguiseUnlocks::new, DisguiseUnlocks::ids);

    public static final StreamCodec<io.netty.buffer.ByteBuf, DisguiseUnlocks> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
                    .map(DisguiseUnlocks::new, DisguiseUnlocks::ids);

    public static DisguiseUnlocks empty() {
        return new DisguiseUnlocks(new ArrayList<>());
    }

    /** 是否已解锁指定怪物外观。 */
    public boolean contains(ResourceLocation id) {
        return id != null && ids.contains(id.toString());
    }

    /** 返回加入指定怪物后的新实例；若已存在则返回自身。 */
    public DisguiseUnlocks with(ResourceLocation id) {
        if (id == null || ids.contains(id.toString())) {
            return this;
        }
        List<String> copy = new ArrayList<>(ids);
        copy.add(id.toString());
        return new DisguiseUnlocks(copy);
    }

    /** 解析为 ResourceLocation 集合（去重、保持顺序）。 */
    public Set<ResourceLocation> resolve() {
        Set<ResourceLocation> set = new LinkedHashSet<>();
        for (String s : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl != null) {
                set.add(rl);
            }
        }
        return set;
    }
}
