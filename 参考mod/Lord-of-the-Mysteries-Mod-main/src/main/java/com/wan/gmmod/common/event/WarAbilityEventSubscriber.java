package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.war.FireWallManager;
import com.wan.gmmod.content.war.InsightManager;
import com.wan.gmmod.content.war.TrackManager;
import com.wan.gmmod.content.war.WarPathwayManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;

/**
 * 战争之红途径（猎人 / 挑衅者 / 纵火家 / 阴谋家）能力相关事件监听：
 * <ul>
 *   <li><b>弱点洞察</b>：对已分析目标伤害 +30%；</li>
 *   <li><b>毒刃 / 注火 / 火焰附魔</b>：近战命中附加中毒 / 体内延时爆炸 / 点燃；</li>
 *   <li><b>火焰抗性</b>：火焰伤害减免 80%（爆炸冲击波正常）；</li>
 *   <li><b>火焰护甲</b>：近战反伤 2 点，冰冻伤害减半；</li>
 *   <li><b>火焰体质</b>：受物理攻击自动化为火焰形态（免疫物理，冰冻翻倍）；</li>
 *   <li><b>激怒失准</b>：被激怒生物攻击有概率落空；</li>
 *   <li><b>野外知识</b>：潜行右键地面免配方书合成药膏 / 毒药，或显现足迹；</li>
 *   <li><b>智力提升 / 情报网</b>：村民交互折扣与情报获取；</li>
 *   <li><b>燃烧之墙</b>：每刻驱动火墙伤害（{@link FireWallManager}）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class WarAbilityEventSubscriber {
    /** 弱点洞察：对已分析目标的伤害加成。 */
    private static final float INSIGHT_BONUS = 0.30F;
    /** 火焰抗性：火焰伤害减免比例。 */
    private static final float FIRE_RESIST_RATIO = 0.80F;
    /** 火焰护甲：近战反伤。 */
    private static final float FLAME_ARMOR_THORNS = 2.0F;
    /** 激怒失准：每级失准概率（I 级 30%，II 级 60%）。 */
    private static final float ENRAGED_MISS_PER_LEVEL = 0.30F;
    /** 情报网：村民交互时的情报获取概率。 */
    private static final float INTEL_CHANCE = 0.30F;

    // ===== 进攻侧：近战命中附加效果 =====

    /** 弱点洞察 +30% / 毒刃 / 注火 / 火焰附魔（近战直接命中判定）。 */
    @SubscribeEvent
    public static void onWarOffense(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer sp)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        long now = sp.level().getGameTime();

        // 弱点洞察：对已完成分析的目标伤害 +30%
        if (SkillManager.isUnlocked(sp, GuimiMod.id("weakness_insight"))
                && InsightManager.isAnalyzed(sp, victim)) {
            event.setAmount(event.getAmount() * (1.0F + INSIGHT_BONUS));
        }
        // 毒刃：基础毒药涂抹剩余次数 > 0 时施加中毒 5 秒并递减
        int poisonHits = sp.getData(ModAttachments.POISON_BLADE_HITS);
        if (poisonHits > 0) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 5 * 20, 0), sp);
            sp.setData(ModAttachments.POISON_BLADE_HITS, poisonHits - 1);
        }
        // 注火：武装窗口内的近战命中登记体内延时爆炸
        if (sp.getData(ModAttachments.FIRE_INJECTION_ARM_END) > now) {
            sp.setData(ModAttachments.FIRE_INJECTION_ARM_END, 0L);
            WarPathwayManager.addInjection(sp, victim, now);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.fire_injection.injected",
                    victim.getDisplayName()), true);
        }
        // 火焰附魔：手持武器自动附带火焰附加 I（点燃 4 秒）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("flame_enchant")) && !victim.fireImmune()) {
            victim.igniteForSeconds(4);
        }
    }

    /** 激怒失准：被激怒生物的攻击按等级概率落空（在加成结算前判定）。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEnragedMiss(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker.level().isClientSide) {
            return;
        }
        MobEffectInstance enraged = attacker.getEffect(ModEffects.ENRAGED);
        if (enraged == null) {
            return;
        }
        float missChance = ENRAGED_MISS_PER_LEVEL * (enraged.getAmplifier() + 1);
        if (attacker.getRandom().nextFloat() < missChance) {
            event.setCanceled(true);
            if (attacker.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6, attacker.getZ(),
                        1, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    // ===== 防御侧：火焰抗性 / 火焰护甲 / 火焰体质 =====

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWarDefense(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide) {
            return;
        }
        long now = sp.level().getGameTime();

        // 火焰抗性：火焰伤害减免 80%（爆炸冲击波正常伤害）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("pyro_fire_resistance"))
                && event.getSource().is(DamageTypeTags.IS_FIRE)
                && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * (1.0F - FIRE_RESIST_RATIO));
        }

        // 火焰护甲：近战攻击者受 2 点反伤，冰冻伤害减半
        if (WarPathwayManager.hasFlameArmor(sp)) {
            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker
                    && attacker != sp && !attacker.fireImmune()) {
                attacker.hurt(sp.damageSources().thorns(sp), FLAME_ARMOR_THORNS);
                attacker.igniteForSeconds(2);
            }
            if (event.getSource().is(DamageTypeTags.IS_FREEZING)) {
                event.setAmount(event.getAmount() * 0.5F);
            }
        }

        // 火焰体质：火焰形态期间免疫物理伤害，冰冻伤害翻倍
        boolean physical = event.getSource().getDirectEntity() != null
                && !event.getSource().is(DamageTypeTags.IS_FIRE)
                && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && !event.getSource().is(DamageTypeTags.WITCH_RESISTANT_TO);
        if (WarPathwayManager.isFireForm(sp)) {
            if (event.getSource().is(DamageTypeTags.IS_FREEZING)) {
                event.setAmount(event.getAmount() * 2.0F);
            } else if (physical) {
                event.setCanceled(true);
                return;
            }
        } else if (physical
                && SkillManager.isUnlocked(sp, GuimiMod.id("fire_body"))
                && sp.getData(ModAttachments.FIRE_FORM_CD_END) <= now) {
            // 自动触发：受物理攻击瞬间化为火焰形态 3 秒，冷却 90 秒
            sp.setData(ModAttachments.FIRE_FORM_END, now + 3 * 20);
            sp.setData(ModAttachments.FIRE_FORM_CD_END, now + 90 * 20);
            sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.5F);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.fire_body.on"), true);
            event.setCanceled(true);
        }
    }

    // ===== 野外知识 / 痕迹显现：潜行右键地面 =====

    @SubscribeEvent
    public static void onSneakUseGround(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer sp)
                || !sp.isShiftKeyDown()) {
            return;
        }
        ItemStack held = event.getItemStack();
        // 野外知识：纸 + 甜浆果 ×2 → 止血药膏；玻璃瓶 + 蜘蛛眼 → 基础毒药
        if (SkillManager.isUnlocked(sp, GuimiMod.id("wild_knowledge"))) {
            if (held.is(Items.PAPER) && sp.getInventory().countItem(Items.SWEET_BERRIES) >= 2) {
                held.shrink(1);
                removeItems(sp, Items.SWEET_BERRIES, 2);
                sp.getInventory().placeItemBackInInventory(new ItemStack(ModItems.HEMOSTATIC_SALVE.get()));
                sp.displayClientMessage(Component.translatable("message.guimi_mod.wild_knowledge.salve"), true);
                event.setCanceled(true);
                return;
            }
            if (held.is(Items.GLASS_BOTTLE) && sp.getInventory().countItem(Items.SPIDER_EYE) >= 1) {
                held.shrink(1);
                removeItems(sp, Items.SPIDER_EYE, 1);
                sp.getInventory().placeItemBackInInventory(new ItemStack(ModItems.BASIC_POISON.get()));
                sp.displayClientMessage(Component.translatable("message.guimi_mod.wild_knowledge.poison"), true);
                event.setCanceled(true);
                return;
            }
        }
        // 痕迹追踪：空手潜行右键地面显现足迹
        if (held.isEmpty() && SkillManager.isUnlocked(sp, GuimiMod.id("trace_tracking"))) {
            TrackManager.reveal(sp);
            event.setCanceled(true);
        }
    }

    // ===== 智力提升 / 情报网：村民交互 =====

    @SubscribeEvent
    public static void onWarVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer sp)
                || !(event.getTarget() instanceof Villager)) {
            return;
        }
        // 智力提升：交易折扣（与教唆者「说服」的折扣叠加，故用更高放大等级）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("intellect_boost"))) {
            sp.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 200, 2, false, false));
        }
        // 情报网：概率获取附近非凡者的模糊坐标情报
        if (SkillManager.isUnlocked(sp, GuimiMod.id("intel_network"))
                && sp.getRandom().nextFloat() < INTEL_CHANCE) {
            sendIntel(sp);
        }
    }

    /** 情报网：汇报 64 米内其他非凡者（已就职玩家）的模糊坐标。 */
    private static void sendIntel(ServerPlayer sp) {
        List<ServerPlayer> beyonders = sp.serverLevel().getPlayers(p -> p != sp && p.isAlive()
                && p.getData(ModAttachments.SEQUENCE_LEVEL) > 0
                && p.distanceToSqr(sp) < 64.0 * 64.0);
        if (beyonders.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.intel_network.nothing"), true);
            return;
        }
        ServerPlayer target = beyonders.get(0);
        // 坐标模糊到 10 格精度
        int fx = (int) (Math.round(target.getX() / 10.0) * 10);
        int fz = (int) (Math.round(target.getZ() / 10.0) * 10);
        sp.sendSystemMessage(Component.translatable("message.guimi_mod.intel_network.report",
                target.getDisplayName(), fx, fz));
    }

    // ===== 燃烧之墙：每刻驱动 =====

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            FireWallManager.tick(level);
        }
    }

    // ===== 辅助 =====

    /** 从背包移除指定数量的物品。 */
    private static void removeItems(Player player, net.minecraft.world.item.Item item, int count) {
        for (int i = 0; i < player.getInventory().getContainerSize() && count > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(count, stack.getCount());
                stack.shrink(take);
                count -= take;
            }
        }
    }
}
