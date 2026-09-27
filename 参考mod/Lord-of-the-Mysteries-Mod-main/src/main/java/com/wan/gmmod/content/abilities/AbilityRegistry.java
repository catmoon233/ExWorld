package com.wan.gmmod.content.abilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class AbilityRegistry {
    // 序列ID -> 能力列表
    private static final Map<ResourceLocation, List<Ability>> SEQUENCE_ABILITIES = new HashMap<>();

    public static void register(ResourceLocation sequenceId, Ability ability) {
        SEQUENCE_ABILITIES.computeIfAbsent(sequenceId, k -> new ArrayList<>()).add(ability);
    }

    /** 获取某序列对应的所有能力（注册表顺序） */
    public static List<Ability> getAbilitiesFor(ResourceLocation sequenceId) {
        return SEQUENCE_ABILITIES.getOrDefault(sequenceId, Collections.emptyList());
    }

    /** 按能力 ID 反查能力实例（技能栏槽位存储的是能力 ID） */
    public static Ability getById(ResourceLocation abilityId) {
        if (abilityId == null) {
            return null;
        }
        for (List<Ability> list : SEQUENCE_ABILITIES.values()) {
            for (Ability ability : list) {
                if (ability.getId().equals(abilityId)) {
                    return ability;
                }
            }
        }
        return null;
    }


    public static java.util.List<Ability> actives() {
        java.util.List<Ability> actives = new java.util.ArrayList<>();
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();
        for (java.util.List<Ability> list : SEQUENCE_ABILITIES.values()) {
            for (Ability ability : list) {
                if (ability.isActive() && seen.add(ability.getId())) actives.add(ability);
            }
        }
        return actives;
    }
    /** 获取玩家当前序列应该拥有的能力 */
    public static List<Ability> getActiveAbilities(Player player, int currentLevel) {
        // 根据玩家当前等级找到对应序列ID，返回能力
        // 这里需要一个反向查找：等级 -> 序列ID
        // 简单实现：遍历所有序列，找到等级匹配的
        // 更好的做法是维护一个等级到ID的映射，但为了清晰，我们先在SequenceRegistry里提供方法
        return List.of(); // 暂时留空，我们将在后面直接使用 SequenceRegistry 搭配
    }
}
