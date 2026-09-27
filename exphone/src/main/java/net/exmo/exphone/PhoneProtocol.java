package net.exmo.exphone;

import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Versioned, section-wise replication. Receipts survive reconnects and world saves. */
final class PhoneProtocol {
    static final int VERSION = 2;
    static final long RETRY_WINDOW = 86_400_000L;
    private static final Map<ServerPlayer, JsonObject> sent = new WeakHashMap<>();

    static void reset(ServerPlayer player) { sent.remove(player); }

    static boolean fresh(String request, long now) {
        try {
            int separator = request.indexOf(':');
            long issued = Long.parseLong(request.substring(0, separator));
            UUID.fromString(request.substring(separator + 1));
            return issued <= now + 60_000 && issued >= now - RETRY_WINDOW;
        } catch (RuntimeException badRequest) { return false; }
    }

    static void result(ServerPlayer player, String request, String code, String message) {
        JsonObject result = new JsonObject();
        result.addProperty("requestId", request);
        result.addProperty("code", code);
        result.addProperty("ok", "OK".equals(code));
        result.addProperty("message", message);
        JsonObject packet = new JsonObject();
        packet.addProperty("version", VERSION);
        packet.add("result", result);
        sendRaw(player, packet);
        if (!request.isBlank() && fresh(request, System.currentTimeMillis())) {
            PhoneData data = PhoneData.get(player);
            data.receipts.put(player.getUUID() + "/" + request, result.toString());
            data.receipts.keySet().removeIf(key -> !fresh(key.substring(key.indexOf('/') + 1), System.currentTimeMillis()));
            data.touch();
        }
    }

    static boolean replay(ServerPlayer player, String request) {
        String result = PhoneData.get(player).receipts.get(player.getUUID() + "/" + request);
        if (result == null) return false;
        JsonObject packet = new JsonObject();
        packet.addProperty("version", VERSION);
        packet.add("result", JsonParser.parseString(result));
        sendRaw(player, packet);
        return true;
    }

    static void send(ServerPlayer player, String json) {
        JsonObject next = JsonParser.parseString(json).getAsJsonObject();
        if (next.has("openMap") || next.has("openWifi") || next.has("openAtm")) { sendRaw(player, next); return; }
        JsonObject previous = sent.computeIfAbsent(player, ignored -> new JsonObject());
        next.addProperty("serverTime", System.currentTimeMillis());
        for (var entry : next.entrySet()) {
            if (entry.getValue().equals(previous.get(entry.getKey())) && !entry.getKey().equals("status")) continue;
            JsonObject patch = new JsonObject();
            patch.addProperty("version", VERSION);
            patch.addProperty("section", entry.getKey());
            patch.add("value", entry.getValue());
            sendRaw(player, patch);
        }
        sent.put(player, next.deepCopy());
    }

    private static void sendRaw(ServerPlayer player, JsonObject packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PhonePayloads.PhoneStatePayload(packet.toString()));
    }

    private PhoneProtocol() {}
}
