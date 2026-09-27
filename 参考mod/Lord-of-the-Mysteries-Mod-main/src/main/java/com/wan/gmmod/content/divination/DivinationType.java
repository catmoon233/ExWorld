package com.wan.gmmod.content.divination;

/**
 * 占卜目标类型，由玩家右键交互方式决定：
 * <ul>
 *     <li>{@link #SELF 内省占卜}：右键空气 / 地面，不指向实体；</li>
 *     <li>{@link #ENTITY 探测占卜}：右键生物或玩家；</li>
 *     <li>{@link #POSITION 地理占卜}：右键方块，获取周围区域信息。</li>
 * </ul>
 */
public enum DivinationType {
    SELF,
    ENTITY,
    POSITION
}
