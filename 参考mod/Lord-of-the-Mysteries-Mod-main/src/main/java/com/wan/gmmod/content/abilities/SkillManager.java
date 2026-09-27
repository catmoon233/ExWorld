package com.wan.gmmod.content.abilities;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.common.capability.data.SkillBarData;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.SequenceRegistry;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能栏运行时逻辑：解锁能力查询、可用性校验、触发，以及盗火人「窃取」的临时能力合并。
 * <p>
 * 大部分方法两端通用（依赖已同步的附件），触发写入仅在服务端调用。
 */
public final class SkillManager {
    private SkillManager() {}

    /** 窃取所得（盗火人临时借用）能力的持久数据键：{id:string, until:long} 列表 */
    private static final String STEAL_GAIN = "gmmod_steal_gain";
    /** 被窃取而暂时失去能力的持久数据键：{id:string, until:long} 列表 */
    private static final String STEAL_LOSS = "gmmod_steal_loss";

    /**
     * 玩家已解锁的能力集合：当前途径下，序列号 ∈ [当前等级, 9] 的所有序列能力之并集。
     * <p>
     * 序列号越小越强，玩家晋升后仍保留较低序列（数字更大）的能力。
     * <p>
     * 升级替代约定：若并集中存在 {@code <path>_advanced} 升级版，则同名基础版被移除，
     * 列表与技能栏中只保留升级版（如序列6的火焰跳跃 45m 替代序列7的 30m 版）。
     * <p>
     * 额外合并「盗火人」机制：临时借用的能力加入集合；被窃取暂时失去的能力从集合移除。
     */
    public static List<Ability> getUnlockedAbilities(Player player) {
        Sequences.Pathway pathway = Sequences.fromKey(player.getData(ModAttachments.PATHWAY));
        int level = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (pathway == null || level < 0) {
            return List.of();
        }
        // 实验性途径未启用时不解锁任何能力（被动不 tick、主动不可触发、HUD 不显示）
        if (com.wan.gmmod.content.exp.ExperimentalPathways.isLocked(pathway)) {
            return List.of();
        }
        LinkedHashSet<Ability> result = new LinkedHashSet<>();
        for (int lv = Sequences.MAX_LEVEL; lv >= level; lv--) {
            Sequence seq = SequenceRegistry.get(pathway, lv);
            if (seq != null) {
                result.addAll(AbilityRegistry.getAbilitiesFor(seq.getId()));
            }
        }
        // 升级替代：存在升级版时移除对应基础版
        List<Ability> list = new ArrayList<>(result);
        list.removeIf(ability -> {
            ResourceLocation adv = advancedIdOf(ability.getId());
            return result.stream().anyMatch(other -> other.getId().equals(adv));
        });
        // 被窃取暂时失去：从解锁集合中移除
        Set<ResourceLocation> blocked = currentlyBlocked(player);
        if (!blocked.isEmpty()) {
            list.removeIf(ability -> blocked.contains(ability.getId()));
        }
        // 盗火人临时借用：追加到解锁集合
        long now = player.level().getGameTime();
        for (StolenEntry entry : readEntries(player, STEAL_GAIN)) {
            if (entry.until() <= now) {
                continue;
            }
            Ability borrowed = AbilityRegistry.getById(entry.id());
            if (borrowed != null && !list.contains(borrowed)) {
                list.add(borrowed);
            }
        }
        return list;
    }

    /** 基础能力 ID 对应的升级版 ID（约定：路径后缀 {@code _advanced}）。 */
    private static ResourceLocation advancedIdOf(ResourceLocation id) {
        return id.withPath(id.getPath() + "_advanced");
    }

    /**
     * 技能栏槽位自动迁移：晋升后槽位里的基础版能力若已被升级版替代，
     * 自动替换为升级版 ID（仅服务端，由玩家 tick 周期性驱动）。
     */
    public static void migrateUpgradedSlots(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        SkillBarData bar = player.getData(ModAttachments.SKILL_BAR);
        SkillBarData updated = bar;
        for (int i = 0; i < SkillBarData.SIZE; i++) {
            ResourceLocation id = bar.get(i);
            if (id == null || isUnlocked(player, id)) {
                continue;
            }
            ResourceLocation adv = advancedIdOf(id);
            if (isUnlocked(player, adv)) {
                updated = updated.with(i, adv);
            }
        }
        if (updated != bar) {
            player.setData(ModAttachments.SKILL_BAR, updated);
        }
    }

    /** 该能力是否属于玩家当前途径与序列（防作弊核心校验）。 */
    public static boolean isUnlocked(Player player, ResourceLocation abilityId) {
        if (abilityId == null) {
            return false;
        }
        for (Ability ability : getUnlockedAbilities(player)) {
            if (ability.getId().equals(abilityId)) {
                return true;
            }
        }
        return false;
    }

    /** 剩余冷却（游戏刻），无冷却返回 0。 */
    public static long cooldownRemaining(Player player, ResourceLocation abilityId) {
        long end = player.getData(ModAttachments.SKILL_COOLDOWNS).getEnd(abilityId);
        return Math.max(0L, end - player.level().getGameTime());
    }

    /** 综合可用性：已解锁 + 灵性足够 + 不在冷却中。 */
    public static boolean canActivate(Player player, Ability ability) {
        return ability != null
                && isUnlocked(player, ability.getId())
                && SpiritualityManager.get(player) >= ability.getSpiritualityCost()
                && cooldownRemaining(player, ability.getId()) <= 0L;
    }

    /**
     * 服务端触发指定槽位的能力：取出能力 ID 后走统一的 {@link #triggerAbility} 流程。
     */
    public static void trigger(Player player, int slot) {
        if (player.level().isClientSide) {
            return;
        }
        SkillBarData bar = player.getData(ModAttachments.SKILL_BAR);
        ResourceLocation id = bar.get(slot);
        if (id == null) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.empty"), true);
            return;
        }
        Ability ability = AbilityRegistry.getById(id);
        if (ability == null) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.empty"), true);
            return;
        }
        if (!ability.isActive()) {
            // 被动能力无法主动释放：给出提示而非静默忽略
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.skill.passive"), true);
            return;
        }
        triggerAbility(player, ability);
    }

    /**
     * 服务端触发指定能力：校验归属 / 失控 / 灵性 / 冷却 → 消耗灵性 → 执行 → 记录冷却。
     * <p>
     * 除技能栏外，也供手持物品右键等快捷触发入口复用（如小丑「飞牌」）。
     */
    public static void triggerAbility(Player player, Ability ability) {
        if (player.level().isClientSide || ability == null || !ability.isActive()) {
            return;
        }
        ResourceLocation id = ability.getId();
        // 失控期间无法使用序列能力
        if (player.hasEffect(com.wan.gmmod.common.registry.ModEffects.LOSING_CONTROL)) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.losing_control"), true);
            return;
        }
        // 防作弊：能力必须属于玩家当前途径与序列（含盗火人临时借用能力）
        if (!isUnlocked(player, id)) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.locked"), true);
            return;
        }
        io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = com.wan.gmmod.content.spell.GuimiSpells.get(id);
        if (spell != io.redspace.ironsspellbooks.api.registry.SpellRegistry.none()
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            spell.attemptInitiateCast(net.minecraft.world.item.ItemStack.EMPTY, 1, serverPlayer.level(),
                    serverPlayer, io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK, true, "sequence");
            return;
        }
        int spirituality = SpiritualityManager.get(player);
        if (spirituality < ability.getSpiritualityCost()) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        long remaining = cooldownRemaining(player, id);
        if (remaining > 0L) {
            player.displayClientMessage(Component.translatable("message.guimi_mod.skill.cooldown",
                    (remaining + 19) / 20), true);
            return;
        }
        // 消耗灵性
        if (ability.getSpiritualityCost() > 0) {
            SpiritualityManager.set(player, spirituality - ability.getSpiritualityCost());
        }
        // 执行能力效果
        ability.onActivate(player);
        // 任务钩子：上报「使用能力」目标进度（type=ability）
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.wan.gmmod.content.quest.QuestManager.report(sp, "ability", id.toString(), 1);
        }
        // 记录冷却：仅「蛛丝蚕茧」保留冷却，其余主动技能取消冷却
        if (ability.getCooldownTicks() > 0 && id.getPath().equals("cocoon")) {
            long now = player.level().getGameTime();
            CooldownData cd = player.getData(ModAttachments.SKILL_COOLDOWNS)
                    .with(id, now + ability.getCooldownTicks(), now);
            player.setData(ModAttachments.SKILL_COOLDOWNS, cd);
        }
    }

    /**
     * 服务端设置槽位能力：清空（id 为 null）或指派已解锁能力（防作弊）。
     */
    public static void configure(Player player, int slot, ResourceLocation abilityId) {
        if (player.level().isClientSide || slot < 0 || slot >= SkillBarData.SIZE) {
            return;
        }
        if (abilityId != null) {
            if (!isUnlocked(player, abilityId)) {
                player.displayClientMessage(Component.translatable("message.guimi_mod.skill.locked"), true);
                return;
            }
            // 被动能力（Marker / TickPassive / PassiveEffect / Aura）无法主动释放，禁止入技能栏
            Ability ability = AbilityRegistry.getById(abilityId);
            if (ability == null || !ability.isActive()) {
                player.displayClientMessage(
                        Component.translatable("message.guimi_mod.skill.passive"), true);
                return;
            }
        }
        SkillBarData bar = player.getData(ModAttachments.SKILL_BAR).with(slot, abilityId);
        player.setData(ModAttachments.SKILL_BAR, bar);
    }

    // ===== 盗火人「窃取」临时能力支持 =====

    /** 记录一条临时借用能力（盗火人窃取所得），到期自动失效。 */
    public static void grantStolenAbility(Player player, ResourceLocation id, long untilTick) {
        addEntry(player, STEAL_GAIN, new StolenEntry(id, untilTick));
    }

    /** 记录一条临时封锁能力（被窃取者的能力暂时失去），到期自动恢复。 */
    public static void blockStolenAbility(Player player, ResourceLocation id, long untilTick) {
        addEntry(player, STEAL_LOSS, new StolenEntry(id, untilTick));
    }

    /** 读取玩家当前「被窃取暂时失去」的能力集合（未过期）。 */
    public static Set<ResourceLocation> currentlyBlocked(Player player) {
        long now = player.level().getGameTime();
        Set<ResourceLocation> set = new HashSet<>();
        for (StolenEntry entry : readEntries(player, STEAL_LOSS)) {
            if (entry.until() > now) {
                set.add(entry.id());
            }
        }
        return set;
    }

    private record StolenEntry(ResourceLocation id, long until) {}

    /** 追加一条持久记录，先清理已过期的条目避免无限增长。 */
    private static void addEntry(Player player, String key, StolenEntry entry) {
        ListTag list = player.getPersistentData().getList(key, Tag.TAG_COMPOUND);
        ListTag cleaned = new ListTag();
        long now = player.level().getGameTime();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.getLong("until") > now) {
                cleaned.add(tag);
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", entry.id().toString());
        tag.putLong("until", entry.until());
        cleaned.add(tag);
        player.getPersistentData().put(key, cleaned);
    }

    private static List<StolenEntry> readEntries(Player player, String key) {
        ListTag list = player.getPersistentData().getList(key, Tag.TAG_COMPOUND);
        List<StolenEntry> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            if (id != null) {
                result.add(new StolenEntry(id, tag.getLong("until")));
            }
        }
        return result;
    }
}
