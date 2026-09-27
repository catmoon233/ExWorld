package com.wan.gmmod.common.network;

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
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Keeps client-only payload handling out of common payload classes.
 *
 * <p>The server still needs to load the payload records while registering the
 * network protocol. Client handlers are registered by the client-only mod
 * entry point, so a dedicated server never resolves a client class.</p>
 */
public final class ClientPayloadDispatcher {
    private static final Map<Class<?>, BiConsumer<?, IPayloadContext>> CLIENT_HANDLERS =
            new ConcurrentHashMap<>();

    private ClientPayloadDispatcher() {
    }

    public static <T> void register(Class<T> packetType, BiConsumer<T, IPayloadContext> handler) {
        CLIENT_HANDLERS.put(packetType, handler);
    }

    @SuppressWarnings("unchecked")
    public static void dispatch(Object packet, IPayloadContext context) {
        BiConsumer<Object, IPayloadContext> handler =
                (BiConsumer<Object, IPayloadContext>) (BiConsumer<?, IPayloadContext>)
                        CLIENT_HANDLERS.get(packet.getClass());
        if (handler != null) {
            handler.accept(packet, context);
        }
    }

    public static void handle(CocoonSyncPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(DistortionModeSyncPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(DistortionZoneSyncPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(HighlightBlocksPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(MarionetteViewPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(MeditationSyncPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(PendulumUsePacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(QuestSyncPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(SpiritThreadSyncPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }

    public static void handle(StealMenuPacket packet, IPayloadContext context) {
        dispatch(packet, context);
    }
}
