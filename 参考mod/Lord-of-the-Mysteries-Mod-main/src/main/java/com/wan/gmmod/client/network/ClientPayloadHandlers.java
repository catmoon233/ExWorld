package com.wan.gmmod.client.network;

import com.wan.gmmod.client.BlockHighlightClientState;
import com.wan.gmmod.client.CocoonClientState;
import com.wan.gmmod.client.DistortionClientState;
import com.wan.gmmod.client.MarionetteControlClientState;
import com.wan.gmmod.client.MeditationClientState;
import com.wan.gmmod.client.PendulumClientState;
import com.wan.gmmod.client.SpiritThreadClientState;
import com.wan.gmmod.client.gui.StealSelectScreen;
import com.wan.gmmod.client.quest.QuestClientState;
import com.wan.gmmod.common.item.PendulumItem;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import com.wan.gmmod.common.network.packet.CocoonSyncPacket;
import com.wan.gmmod.common.network.packet.DistortionModeSyncPacket;
import com.wan.gmmod.common.network.packet.DistortionZoneSyncPacket;
import com.wan.gmmod.common.network.packet.HighlightBlocksPacket;
import com.wan.gmmod.common.network.packet.MarionetteViewPacket;
import com.wan.gmmod.common.network.packet.MeditationSyncPacket;
import com.wan.gmmod.common.network.packet.PendulumUsePacket;
import com.wan.gmmod.common.network.packet.QuestSyncPacket;
import com.wan.gmmod.common.network.packet.StealMenuPacket;
import com.wan.gmmod.common.network.packet.SpiritThreadSyncPacket;
import com.wan.gmmod.content.divination.PendulumSpin;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client-only implementations for server-to-client payloads. */
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    /** Registers all server-to-client payload handlers during client mod setup. */
    public static void register() {
        ClientPayloadDispatcher.register(CocoonSyncPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(DistortionModeSyncPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(DistortionZoneSyncPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(HighlightBlocksPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(MarionetteViewPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(MeditationSyncPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(PendulumUsePacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(QuestSyncPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(SpiritThreadSyncPacket.class, ClientPayloadHandlers::handle);
        ClientPayloadDispatcher.register(StealMenuPacket.class, ClientPayloadHandlers::handle);
    }

    public static void handle(CocoonSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.burning()) {
                CocoonClientState.burst(packet.entityId(), packet.remainingTicks());
            } else {
                CocoonClientState.enclose(packet.entityId(), packet.remainingTicks());
            }
        });
    }

    public static void handle(DistortionModeSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> DistortionClientState.setModeActive(packet.active()));
    }

    public static void handle(DistortionZoneSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                DistortionClientState.addZoneOutlines(packet.positions(), packet.duration()));
    }

    public static void handle(HighlightBlocksPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> BlockHighlightClientState.add(packet.positions(), packet.duration()));
    }

    public static void handle(MarionetteViewPacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                MarionetteControlClientState.setControlling(packet.active(), packet.entityId()));
    }

    public static void handle(MeditationSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> MeditationClientState.setMeditating(packet.meditating()));
    }

    public static void handle(PendulumUsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player localPlayer = context.player();
            if (localPlayer == null) {
                return;
            }
            Entity entity = localPlayer.level().getEntity(packet.entityId());
            if (entity instanceof Player user) {
                PendulumSpin spin = PendulumSpin.byId(packet.spinId());
                PendulumClientState.startUsing(user.getUUID(), PendulumUsePacket.DURATION);
                PendulumItem.triggerUseAnimation(spin);
            }
        });
    }

    public static void handle(QuestSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> QuestClientState.load(packet.tasksJson()));
    }

    public static void handle(SpiritThreadSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> SpiritThreadClientState.update(
                packet.marionetteId(), packet.targetId(), packet.struggling()));
    }

    public static void handle(StealMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new StealSelectScreen(packet.targetId(), packet.labels())));
    }
}
