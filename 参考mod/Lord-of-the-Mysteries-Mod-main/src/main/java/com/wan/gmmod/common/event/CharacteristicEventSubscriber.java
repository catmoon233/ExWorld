package com.wan.gmmod.common.event;

import com.wan.gmmod.Config;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.MagicArtifactItem;
import com.wan.gmmod.common.item.SealedArtifactItem;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 非凡特性事件监听（不灭定律 + 聚合定律 · 生成偏向 + 守恒定律 · 死亡返还）。
 * <ul>
 *     <li><b>不灭定律</b>：承载特性的生物死亡 → 生成特性物品（不消灭，可被聚合吸引）；
 *         特性物品即将过期 / 坠入虚空 → 取消过期 / 传送保护。</li>
 *     <li><b>聚合定律 · 生成偏向</b>：自然生成的生物若附近有携带相近途径特性的玩家，
 *         有概率被标记为该途径低序列特性承载者。</li>
 *     <li><b>守恒定律 · 死亡返还</b>：玩家死亡后序列等级下降（变弱），流失的高序列特性
 *         作为物品掉落在死亡点，全局池高序列 -1、低序列 +1。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class CharacteristicEventSubscriber {

    /** 生成偏向触发概率 */
    private static final float SPAWN_BIAS_CHANCE = 0.05f;

    // ===== 守恒定律：世界首次加载时按配置初始化全局池 =====
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            CharacteristicManager.initWorldPool(level);
        }
    }

    // ===== 不灭定律：承载特性的生物死亡 → 生成特性物品 =====
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || dead instanceof Player) {
            return;
        }
        CharacteristicData carried = CharacteristicManager.getCarrier(dead);
        if (carried == null) {
            return;
        }
        Sequences.Pathway pathway = Sequences.fromKey(carried.pathway());
        if (pathway == null) {
            return;
        }
        // 特性不随生物消灭：有概率与尸体附近的耐久物品融合成封印物 / 神奇物品，
        // 否则在尸体位置析出对应特性物品（并可被同/相近途径玩家吸引）
        if (!tryFuseIntoArtifact(dead, pathway, carried.level())) {
            CharacteristicManager.spawnCharacteristic(dead.level(), dead.position(), pathway, carried.level());
        }
    }

    // ===== 聚合定律 · 死亡融合：特性与附近物品融合成封印物 / 神奇物品 =====

    /** 附近可融合的掉落物搜索半径（方块）。 */
    private static final double FUSION_ITEM_RANGE = 3.0;

    /**
     * 以配置概率尝试将承载特性与附近耐久物品融合成超凡物品。
     * <p>
     * 门途径（{@link Sequences.Pathway#DOOR}）特性融合成「神奇物品」，
     * 其余途径融合成「封印物」。成功则消费地上的基底物品并在尸体处生成物品，
     * 返回 {@code true}；无可融合物品或未命中概率则返回 {@code false}（特性照常析出）。
     */
    private static boolean tryFuseIntoArtifact(LivingEntity dead, Sequences.Pathway pathway, int level) {
        if (dead.getRandom().nextDouble() >= Config.CHARACTERISTIC_DEATH_FUSION_CHANCE.get()) {
            return false;
        }
        ItemEntity target = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity ie : dead.level().getEntitiesOfClass(ItemEntity.class,
                new AABB(dead.position(), dead.position()).inflate(FUSION_ITEM_RANGE))) {
            if (!SealedArtifactItem.isSealableBase(ie.getItem())) {
                continue;
            }
            double d = ie.distanceToSqr(dead);
            if (d < best) {
                best = d;
                target = ie;
            }
        }
        if (target == null) {
            return false;
        }
        ItemStack base = target.getItem();
        CharacteristicData data = new CharacteristicData(pathway.getKey(), level);
        ItemStack artifact = pathway == Sequences.Pathway.DOOR
                ? MagicArtifactItem.create(base, data, dead.level())
                : SealedArtifactItem.create(base, data);
        base.shrink(1);
        if (base.isEmpty()) {
            target.discard();
        } else {
            target.setItem(base);
        }
        ItemEntity out = new ItemEntity(dead.level(),
                dead.getX(), dead.getY() + 0.5, dead.getZ(), artifact);
        out.setPickUpDelay(20);
        dead.level().addFreshEntity(out);
        return true;
    }

    // ===== 不灭定律：特性 / 封印物物品不会自然过期消失 =====
    @SubscribeEvent
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity item = event.getEntity();
        if (isIndestructibleItem(item.getItem())) {
            // 延长寿命（每次即将过期时重新延期），使特性/封印物物品不会自然消失，
            // 只能被拾取、用于魔药调配或封印合成
            event.setExtraLife(6000);
        }
    }

    /** 不灭物品：非凡特性或封印物。 */
    private static boolean isIndestructibleItem(ItemStack stack) {
        return CharacteristicManager.isIndestructibleItem(stack);
    }

    // ===== 不灭定律：特性 / 封印物物品坠入虚空时传送保护 =====
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity item) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!isIndestructibleItem(item.getItem())) {
            return;
        }
        if (item.getY() >= level.getMinBuildHeight() - 3) {
            return;
        }
        // 即将坠入虚空：传送到最近玩家，否则回到世界出生点，避免特性被虚空销毁
        Player nearest = level.getNearestPlayer(item, 256.0);
        if (nearest != null) {
            item.teleportTo(nearest.getX(), nearest.getY() + 1.0, nearest.getZ());
        } else {
            var spawn = level.getSharedSpawnPos();
            item.teleportTo(spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5);
        }
        item.setDeltaMovement(Vec3.ZERO);
    }

    // ===== 聚合定律 · 生成偏向：自然生成的生物偏向承载相近途径特性 =====
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (CharacteristicManager.isCarrier(mob) || mob.getRandom().nextFloat() >= SPAWN_BIAS_CHANCE) {
            return;
        }
        // 偏向就近玩家的途径（半径随附近特性物品数量成长）
        double range = CharacteristicManager.effectiveAggregationRange(level, mob.position(),
                Config.CHARACTERISTIC_ATTRACTION_RANGE.get());
        for (Player player : level.players()) {
            Sequences.Pathway pp = Sequences.fromKey(player.getData(ModAttachments.PATHWAY));
            if (pp == null) {
                continue;
            }
            if (player.distanceToSqr(mob) <= range * range) {
                CharacteristicManager.markCarrier(mob, pp, Sequences.MAX_LEVEL);
                break;
            }
        }
    }

    // ===== 守恒定律 · 死亡返还：玩家死亡 → 序列等级下降 + 掉落流失特性 =====
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Player original = event.getOriginal();
        Player updated = event.getEntity();
        int curLevel = original.getData(ModAttachments.SEQUENCE_LEVEL);
        Sequences.Pathway pathway = Sequences.fromKey(original.getData(ModAttachments.PATHWAY));
        // 仅当已就职且尚有降级空间（未处于最弱序列 9）时才流失
        if (pathway == null || curLevel <= 0 || curLevel >= Sequences.MAX_LEVEL) {
            return;
        }
        int newLevel = curLevel + 1; // 数字越大越弱：死亡后变弱一级

        updated.setData(ModAttachments.SEQUENCE_LEVEL, newLevel);
        updated.setData(ModAttachments.ACTING_SEQUENCE_ID, pathway.sequenceId(newLevel).toString());
        // 扮演进度重置，需要重新在新序列扮演
        updated.setData(ModAttachments.ACTING_PROGRESS, 0);

        if (original.level() instanceof ServerLevel level) {
            // 守恒：高序列 -1、低序列 +1；流失的高序列特性掉在死亡点
            CharacteristicManager.transferDown(level, pathway, curLevel, newLevel);
            CharacteristicManager.spawnCharacteristic(level, original.position(), pathway, curLevel);
        }
    }
}
