package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.AbilityTargeting;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 弱点洞察管理器——猎人「弱点洞察」/ 挑衅者「洞察弱点（升级）」的观察与分析状态。
 * <p>
 * 潜行注视同一目标满 3 秒（60 刻）即完成分析：
 * <ul>
 *   <li>对该目标的攻击伤害 +30%（判定在 WarAbilityEventSubscriber）；</li>
 *   <li>目标获得发光轮廓（碰撞箱高亮）；</li>
 *   <li>目标类型记入 {@code KNOWN_MOBS}（挑衅「掌握情报」判据）；</li>
 *   <li>升级版额外显示目标的「敏感词条」。</li>
 * </ul>
 * 全部为服务端瞬态状态，服务器重启后需重新观察（KNOWN_MOBS 除外，随玩家持久化）。
 */
public final class InsightManager {

    /** 观察进度需求：3 秒。 */
    private static final int OBSERVE_TICKS = 60;
    /** 观察拾取距离。 */
    private static final double OBSERVE_RANGE = 20.0;

    /** 玩家 → 当前观察目标。 */
    private static final Map<UUID, UUID> OBSERVING = new HashMap<>();
    /** 玩家 → 观察进度（刻）。 */
    private static final Map<UUID, Integer> PROGRESS = new HashMap<>();
    /** 玩家 → 已完成分析的目标集合。 */
    private static final Map<UUID, Set<UUID>> ANALYZED = new HashMap<>();

    private InsightManager() {
    }

    /** 每刻调用（已确认解锁弱点洞察）：推进潜行观察进度。 */
    public static void tick(ServerPlayer sp) {
        if (!sp.isShiftKeyDown()) {
            OBSERVING.remove(sp.getUUID());
            PROGRESS.remove(sp.getUUID());
            return;
        }
        LivingEntity target = AbilityTargeting.pickLivingEntity(sp, OBSERVE_RANGE);
        if (target == null) {
            OBSERVING.remove(sp.getUUID());
            PROGRESS.remove(sp.getUUID());
            return;
        }
        UUID prev = OBSERVING.get(sp.getUUID());
        if (!target.getUUID().equals(prev)) {
            // 换目标：重新计时
            OBSERVING.put(sp.getUUID(), target.getUUID());
            PROGRESS.put(sp.getUUID(), 0);
            return;
        }
        if (isAnalyzed(sp, target)) {
            // 已分析目标：持续注视时刷新轮廓高亮
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
            return;
        }
        int progress = PROGRESS.merge(sp.getUUID(), 1, Integer::sum);
        if (progress >= OBSERVE_TICKS) {
            completeAnalysis(sp, target);
        }
    }

    private static void completeAnalysis(ServerPlayer sp, LivingEntity target) {
        ANALYZED.computeIfAbsent(sp.getUUID(), k -> new HashSet<>()).add(target.getUUID());
        PROGRESS.remove(sp.getUUID());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, true, false));
        markKnown(sp, target);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.insight.analyzed",
                target.getDisplayName()), true);
        // 洞察弱点（升级）：额外显示敏感词条
        if (SkillManager.isUnlocked(sp, GuimiMod.id("weakness_insight_advanced"))) {
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.insight.traits",
                    target.getDisplayName(), sensitiveTraits(target)));
        }
    }

    /** 该目标是否已被玩家完成分析（攻击伤害 +30% 判据）。 */
    public static boolean isAnalyzed(Player player, LivingEntity target) {
        Set<UUID> set = ANALYZED.get(player.getUUID());
        return set != null && set.contains(target.getUUID());
    }

    /** 玩家是否已掌握该生物类型的情报（挑衅时长判据）。 */
    public static boolean isKnown(Player player, LivingEntity target) {
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        return Arrays.asList(player.getData(ModAttachments.KNOWN_MOBS).split(",")).contains(typeId);
    }

    /** 将目标类型记入已掌握情报（KNOWN_MOBS，逗号分隔去重）。 */
    public static void markKnown(Player player, LivingEntity target) {
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        String known = player.getData(ModAttachments.KNOWN_MOBS);
        if (!Arrays.asList(known.split(",")).contains(typeId)) {
            player.setData(ModAttachments.KNOWN_MOBS, known.isEmpty() ? typeId : known + "," + typeId);
        }
    }

    /** 生成目标的「敏感词条」描述（升级版洞察显示）。 */
    private static Component sensitiveTraits(LivingEntity target) {
        StringBuilder sb = new StringBuilder();
        if (target.fireImmune() || target instanceof Blaze) {
            sb.append(Component.translatable("trait.guimi_mod.fire_immune").getString());
        } else {
            sb.append(Component.translatable("trait.guimi_mod.fears_fire").getString());
        }
        if (target.isInvertedHealAndHarm()) {
            sb.append(" / ").append(Component.translatable("trait.guimi_mod.undead").getString());
        }
        if (target.isSensitiveToWater()) {
            sb.append(" / ").append(Component.translatable("trait.guimi_mod.fears_water").getString());
        }
        if (target instanceof Player) {
            sb.append(" / ").append(Component.translatable("trait.guimi_mod.beyonder").getString());
        } else if (target instanceof Mob mob && mob.getTarget() == null) {
            sb.append(" / ").append(Component.translatable("trait.guimi_mod.unaware").getString());
        }
        return Component.literal(sb.toString());
    }

    /** 玩家离线 / 死亡清理观察进度（已分析集合保留至服务器重启）。 */
    public static void clearObserving(UUID playerId) {
        OBSERVING.remove(playerId);
        PROGRESS.remove(playerId);
    }
}
