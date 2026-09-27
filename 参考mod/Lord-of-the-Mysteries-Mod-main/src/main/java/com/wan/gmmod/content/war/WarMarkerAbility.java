package com.wan.gmmod.content.war;

import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.resources.ResourceLocation;

/**
 * 战争之红途径的通用被动标记能力。
 * <p>
 * 自身不承载逻辑，仅作为「已解锁」标记供 {@code WarPathwayManager} /
 * {@code WarAbilityEventSubscriber} / 物品与客户端层查询（如痕迹追踪、
 * 野外知识、火焰塑形、炽白压缩强化等）。
 */
public class WarMarkerAbility extends Ability {

    public WarMarkerAbility(ResourceLocation id) {
        super(id);
    }
}
