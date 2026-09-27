package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.packet.QuestSyncPacket;
import com.wan.gmmod.content.quest.QuestManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * 任务行为事件挂接：击杀 / 拾取 / 合成 / 结构探索 → 上报任务进度。
 * <p>
 * 全部上报经由 {@link QuestManager#report}，仅在服务端触发。
 */
// Quests are disabled.
public class QuestEventSubscriber {

    /** 玩家登录：下发任务定义到客户端；首次进入时发放任务书。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new QuestSyncPacket(QuestSyncPacket.serialize()));
        }
    }

    /** 击杀目标（type=kill）：target 为实体类型 ID。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
            QuestManager.report(player, "kill", type, 1);
        }
    }

    /** 拾取物品（type=collect）：target 为物品 ID，数量取实际拾取量。 */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            String itemId = BuiltInRegistries.ITEM.getKey(
                    event.getOriginalStack().getItem()).toString();
            QuestManager.report(player, "collect", itemId, event.getOriginalStack().getCount());
        }
    }

    /** 合成物品（type=craft）：target 为产物物品 ID。 */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String itemId = BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).toString();
            QuestManager.report(player, "craft", itemId, event.getCrafting().getCount());
        }
    }

    /** 结构探索（type=explore）：target 为结构 ID。每 20 刻检测一次玩家所在结构。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || player.tickCount % 20 != 0) {
            return;
        }
        StructureManager structures = level.structureManager();
        Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> found = structures.getAllStructuresAt(player.blockPosition());
        if (found.isEmpty()) {
            return;
        }
        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure structure : found.keySet()) {
            ResourceLocation key = structureRegistry.getKey(structure);
            if (key != null) {
                QuestManager.report(player, "explore", key.toString(), 1);
            }
        }
    }
}
