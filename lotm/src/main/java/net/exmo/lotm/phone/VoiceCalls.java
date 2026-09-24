package net.exmo.lotm.phone;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Optional Simple Voice Chat call. Players in the isolated group hear only each other. */
final class VoiceCalls {
    private static final String API = "de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl";
    private static final Map<UUID, UUID> peers = new ConcurrentHashMap<>();
    private static final Map<UUID, String> names = new ConcurrentHashMap<>();

    private VoiceCalls() {}

    static boolean installed() {
        try {
            Class.forName(API);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    static String partner(ServerPlayer player) {
        UUID peer = peers.get(player.getUUID());
        return peer == null ? "" : names.getOrDefault(peer, "");
    }

    static String call(ServerPlayer caller, String targetName) {
         if (!installed()) return "Simple Voice Chat 尚未加入，电话接入已预留";
        ServerPlayer target = null;
        for (ServerPlayer online : caller.server.getPlayerList().getPlayers()) {
            if (online.getGameProfile().getName().equalsIgnoreCase(targetName)) target = online;
        }
        if (target == null || target == caller) return "对方不在线";
        try {
            Object api = Class.forName(API).getMethod("instance").invoke(null);
            Object builder = api.getClass().getMethod("groupBuilder").invoke(api);
            Class<?> type = Class.forName("de.maxhenkel.voicechat.api.Group$Type");
            Object isolated = type.getField("ISOLATED").get(null);
            builder = builder.getClass().getMethod("setName", String.class).invoke(builder, "手机-" + caller.getGameProfile().getName());
            builder = builder.getClass().getMethod("setHidden", boolean.class).invoke(builder, true);
            builder = builder.getClass().getMethod("setPersistent", boolean.class).invoke(builder, false);
            builder = builder.getClass().getMethod("setType", type).invoke(builder, isolated);
            Object group = builder.getClass().getMethod("build").invoke(builder);
            if (!join(api, caller, group) || !join(api, target, group)) {
                remove(api, group);
                return "对方没有连接语音";
            }
            remember(caller, target);
            return "正在呼叫 " + target.getGameProfile().getName();
        } catch (ReflectiveOperationException exception) {
            return "语音通话失败";
        }
    }

    static String hangup(ServerPlayer player) {
         if (!installed()) return "Simple Voice Chat 尚未加入，电话接入已预留";
        UUID peer = peers.remove(player.getUUID());
        if (peer != null) peers.remove(peer);
        try {
            Object api = Class.forName(API).getMethod("instance").invoke(null);
            Object connection = api.getClass().getMethod("getConnectionOf", UUID.class).invoke(api, player.getUUID());
            if (connection == null) return "语音未连接";
            Object group = connection.getClass().getMethod("getGroup").invoke(connection);
            connection.getClass().getMethod("setGroup", Class.forName("de.maxhenkel.voicechat.api.Group")).invoke(connection, new Object[] {null});
            remove(api, group);
            return "已挂断";
        } catch (ReflectiveOperationException exception) {
            return "挂断失败";
        }
    }

    private static void remember(ServerPlayer caller, ServerPlayer target) {
        peers.put(caller.getUUID(), target.getUUID());
        peers.put(target.getUUID(), caller.getUUID());
        names.put(caller.getUUID(), caller.getGameProfile().getName());
        names.put(target.getUUID(), target.getGameProfile().getName());
    }

    private static void remove(Object api, Object group) throws ReflectiveOperationException {
        if (group == null) return;
        Object id = group.getClass().getMethod("getId").invoke(group);
        api.getClass().getMethod("removeGroup", UUID.class).invoke(api, id);
    }

    private static boolean join(Object api, ServerPlayer player, Object group) throws ReflectiveOperationException {
        Object connection = api.getClass().getMethod("getConnectionOf", UUID.class).invoke(api, player.getUUID());
        if (connection == null) return false;
        Method setGroup = connection.getClass().getMethod("setGroup", Class.forName("de.maxhenkel.voicechat.api.Group"));
        setGroup.invoke(connection, group);
        return true;
    }

    /** Reserved SVC group hook. Uses the mod when it is present; does not implement voice itself. */
    static String group(ServerPlayer host, String name, java.util.List<ServerPlayer> members) {
         if (!installed()) return "Simple Voice Chat 尚未加入，群语音接入已预留";
        if (members.size() < 2) return "群里没有足够的在线成员";
        try {
            Object api = Class.forName(API).getMethod("instance").invoke(null);
            Object builder = api.getClass().getMethod("groupBuilder").invoke(api);
            Class<?> type = Class.forName("de.maxhenkel.voicechat.api.Group$Type");
            Object isolated = type.getField("ISOLATED").get(null);
            builder = builder.getClass().getMethod("setName", String.class).invoke(builder, "群聊-" + name);
            builder = builder.getClass().getMethod("setHidden", boolean.class).invoke(builder, true);
            builder = builder.getClass().getMethod("setPersistent", boolean.class).invoke(builder, false);
            builder = builder.getClass().getMethod("setType", type).invoke(builder, isolated);
            Object group = builder.getClass().getMethod("build").invoke(builder);
            int joined = 0;
            for (ServerPlayer member : members) if (join(api, member, group)) joined++;
            if (joined < 2) {
                remove(api, group);
                return "成员没有连接语音";
            }
            return "群语音已接通 " + joined + " 人";
        } catch (ReflectiveOperationException exception) {
            return "语音通话失败";
        }
    }
}
