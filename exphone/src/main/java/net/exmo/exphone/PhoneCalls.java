package net.exmo.exphone;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电话通话状态机（服务端唯一事实来源）。
 * 状态：空闲 → 正在呼叫（振铃 30 秒）→ 通话中；任何一端挂断、离线、手机没电、
 * 手机不在物品栏都会自动结束通话。语音传输不走 SVC 群组：{@code PhoneVoicePlugin}
 * 在服务端把双方麦克风包点对点转发为静态音频，并隔离周边的其他语音。
 * 计费按分钟（不按刻）：主叫每分钟扣 1 话费，双方手机每分钟消耗 1% 电量。
 */
public final class PhoneCalls {
    private static final long RING_MILLIS = 30_000L;
    private static final long BILL_MILLIS = 60_000L;
    private static final int RATE = 1;

    private static final String KEY_RING_OUT = "message.exphone.phone.ring_out";
    private static final String KEY_RING_IN = "message.exphone.phone.ring_in";
    private static final String KEY_LIVE = "message.exphone.phone.call_live";
    private static final String KEY_END = "message.exphone.phone.call_end";
    private static final String KEY_MISSED = "message.exphone.phone.call_missed";
    private static final String KEY_HANGUP = "message.exphone.phone.call.hangup";
    private static final String KEY_ACCEPT = "message.exphone.phone.call.accept";
    private static final String END_MANUAL = "message.exphone.phone.end_manual";
    private static final String END_BATTERY = "message.exphone.phone.end_battery";
    private static final String END_PHONE = "message.exphone.phone.end_phone";
    private static final String END_CREDIT = "message.exphone.phone.end_credit";
    private static final String END_OFFLINE = "message.exphone.phone.end_offline";
    private static final String END_VOICE = "message.exphone.phone.end_voice";
    private static final String END_SERVER = "message.exphone.phone.end_server";

    private static final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private static volatile boolean attached;

    private PhoneCalls() {}

    /** 由 SVC 插件在 initialize 阶段回写，表示语音模组已加载并接管通话路由。 */
    static void attach() {
        attached = true;
    }

    static boolean installed() {
        return attached;
    }

    public static boolean isInCall(UUID player) {
        return sessions.containsKey(player);
    }

    public static UUID peerOf(UUID player) {
        Session session = sessions.get(player);
        if (session == null) return null;
        return session.caller.equals(player) ? session.target : session.caller;
    }

    public static String partner(ServerPlayer player) {
        Session session = sessions.get(player.getUUID());
        if (session == null) return "";
        return session.caller.equals(player.getUUID()) ? session.targetName : session.callerName;
    }

    public static String call(ServerPlayer caller, String targetName) {
        if (!installed()) return "未安装 Simple Voice Chat";
        MinecraftServer server = caller.server;
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
        if (target == null || target == caller) return "对方不在线";
        expire(server);
        if (sessions.containsKey(caller.getUUID()) || sessions.containsKey(target.getUUID())) return "正在通话中";
        Session session = new Session(server, caller.getUUID(), target.getUUID(),
                caller.getGameProfile().getName(), target.getGameProfile().getName());
        session.phase = Phase.RINGING;
        session.ringUntil = System.currentTimeMillis() + RING_MILLIS;
        sessions.put(session.caller, session);
        sessions.put(session.target, session);
        say(caller, ringOut(targetName));
        say(target, ringIn(session.callerName));
        PhoneSystem.send(target, PhoneActions.snapshot(target, session.callerName + " 来电"));
        return "正在呼叫 " + targetName;
    }

    public static String accept(ServerPlayer target) {
        if (!installed()) return "未安装 Simple Voice Chat";
        MinecraftServer server = target.server;
        expire(server);
        Session session = sessions.get(target.getUUID());
        if (session == null || session.phase != Phase.RINGING || !session.target.equals(target.getUUID())) return "没有待接听的来电";
        ServerPlayer caller = server.getPlayerList().getPlayer(session.caller);
        if (caller == null) {
            end(session, END_OFFLINE);
            return "对方已离线";
        }
        ItemStack targetPhone = PhoneStacks.phoneOf(target);
        if (targetPhone.isEmpty()) {
            end(session, END_PHONE);
            return "手机不在物品栏";
        }
        if (PhoneStacks.battery(targetPhone) <= 0) {
            end(session, END_BATTERY);
            return "手机电量不足";
        }
        if (!PhoneVoicePlugin.canConnect(session.caller, session.target)) return "对方没有连接语音";
        session.phase = Phase.ACTIVE;
        session.startedAt = System.currentTimeMillis();
        if (!charge(server, session)) {
            end(session, END_CREDIT);
            return "对方话费不足，无法接通";
        }
        String status = "已接通 · 计费 " + RATE + " 话费/分钟";
        push(caller, status);
        push(target, status);
        say(caller, live());
        say(target, live());
        return "已接通";
    }

    /** 用户主动挂断（手机界面按钮或聊天栏点击文字/命令）。 */
    public static String hangup(ServerPlayer player) {
        Session session = sessions.remove(player.getUUID());
        if (session == null) return "当前没有通话";
        sessions.remove(session.caller);
        sessions.remove(session.target);
        ServerPlayer other = session.server.getPlayerList().getPlayer(
                session.caller.equals(player.getUUID()) ? session.target : session.caller);
        Component reason = Component.translatable(END_MANUAL);
        say(player, Component.translatable(KEY_END, reason));
        push(player, "通话已结束");
        if (other != null) {
            say(other, Component.translatable(KEY_END, reason));
            push(other, "通话已结束");
        }
        return "已挂断";
    }

    public static JsonObject state(ServerPlayer player) {
        JsonObject json = new JsonObject();
        Session session = sessions.get(player.getUUID());
        if (session == null) {
            json.addProperty("phase", "idle");
            json.addProperty("peer", "");
            json.addProperty("startedAt", 0L);
            json.addProperty("charged", 0L);
            return json;
        }
        boolean callerSide = session.caller.equals(player.getUUID());
        String phase = session.phase == Phase.ACTIVE ? "active" : callerSide ? "outgoing" : "incoming";
        json.addProperty("phase", phase);
        json.addProperty("peer", callerSide ? session.targetName : session.callerName);
        json.addProperty("startedAt", session.phase == Phase.ACTIVE ? session.startedAt : 0L);
        json.addProperty("charged", session.charged);
        return json;
    }

    /** 每 20 tick（1 秒）跑一次：振铃超时、手机状态检查、按分钟计费。 */
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0 || sessions.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Session session : List.copyOf(sessions.values())) {
            if (!sessions.containsKey(session.caller)) continue; // this tick already ended it
            if (session.phase == Phase.RINGING && now >= session.ringUntil) {
                missed(session);
                continue;
            }
            if (!sound(server, session)) continue;
            if (session.phase == Phase.ACTIVE && now - session.lastCharge >= BILL_MILLIS) {
                if (!charge(server, session)) {
                    end(session, END_CREDIT);
                    continue;
                }
                push(server, session.caller, "通话计费 · 已通话 " + session.charged + " 分钟");
                push(server, session.target, "通话中 · " + session.charged + " 分钟");
            }
        }
    }

    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Session session = sessions.get(player.getUUID());
        if (session != null) end(session, END_OFFLINE);
    }

    /** SVC 插件报告某个玩家语音断开了。 */
    public static void onVoiceOffline(UUID player) {
        Session session = sessions.get(player);
        if (session != null) end(session, END_VOICE);
    }

    /** SVC 语音服务停止时结束所有通话。 */
    public static void onVoiceServerStopped() {
        for (Session session : List.copyOf(sessions.values())) end(session, END_SERVER);
    }

    private static void expire(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (Session session : List.copyOf(sessions.values())) {
            if (session.phase == Phase.RINGING && now >= session.ringUntil) missed(session);
        }
    }

    /** 手机必须还在物品栏且有电，否则结束通话。 */
    private static boolean sound(MinecraftServer server, Session session) {
        for (UUID id : List.of(session.caller, session.target)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                end(session, END_OFFLINE);
                return false;
            }
            ItemStack phone = PhoneStacks.phoneOf(player);
            if (phone.isEmpty()) {
                end(session, END_PHONE);
                return false;
            }
            if (PhoneStacks.battery(phone) <= 0) {
                end(session, END_BATTERY);
                return false;
            }
        }
        return true;
    }

    /** 按分钟计费：主叫扣话费、双方各消耗 1% 电量。失败返回 false（话费不足）。 */
    private static boolean charge(MinecraftServer server, Session session) {
        ServerPlayer caller = server.getPlayerList().getPlayer(session.caller);
        ServerPlayer target = server.getPlayerList().getPlayer(session.target);
        if (caller == null || target == null) return false;
        PhoneData data = PhoneData.get(caller);
        PhoneData.Profile self = data.ensure(caller);
        PhoneData.Sim sim = self.number.isBlank() ? null : data.balance(self.number);
        if (sim != null) {
            if (sim.credit <= 0) return false;
            sim.credit -= RATE;
            data.touch();
        }
        PhoneStacks.drain(caller);
        PhoneStacks.drain(target);
        session.charged++;
        session.lastCharge = System.currentTimeMillis();
        return true;
    }

    private static void missed(Session session) {
        sessions.remove(session.caller);
        sessions.remove(session.target);
        ServerPlayer caller = session.server.getPlayerList().getPlayer(session.caller);
        ServerPlayer target = session.server.getPlayerList().getPlayer(session.target);
        if (caller != null) {
            say(caller, Component.translatable(KEY_MISSED));
            push(caller, "对方未接听");
        }
        if (target != null) push(target, "未接来电");
    }

    private static void end(Session session, String reasonKey) {
        sessions.remove(session.caller);
        sessions.remove(session.target);
        ServerPlayer caller = session.server.getPlayerList().getPlayer(session.caller);
        ServerPlayer target = session.server.getPlayerList().getPlayer(session.target);
        Component reason = Component.translatable(reasonKey);
        Component line = Component.translatable(KEY_END, reason);
        if (caller != null) {
            say(caller, line);
            push(caller, reason.getString());
        }
        if (target != null) {
            say(target, line);
            push(target, reason.getString());
        }
    }

    private static void push(ServerPlayer player, String status) {
        PhoneSystem.send(player, PhoneActions.snapshot(player, status));
    }

    private static void push(MinecraftServer server, UUID id, String status) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) push(player, status);
    }

    private static void say(ServerPlayer player, Component text) {
        player.displayClientMessage(text, false);
    }

    private static Component ringOut(String name) {
        return Component.translatable(KEY_RING_OUT, name).append(Component.literal(" "))
                .append(link(KEY_HANGUP, "/exphone hangup", ChatFormatting.RED));
    }

    private static Component ringIn(String name) {
        return Component.translatable(KEY_RING_IN, name).append(Component.literal(" "))
                .append(link(KEY_ACCEPT, "/exphone accept", ChatFormatting.GREEN))
                .append(Component.literal(" / "))
                .append(link(KEY_HANGUP, "/exphone hangup", ChatFormatting.RED));
    }

    private static Component live() {
        return Component.translatable(KEY_LIVE).append(Component.literal(" "))
                .append(link(KEY_HANGUP, "/exphone hangup", ChatFormatting.RED));
    }

    private static Component link(String key, String command, ChatFormatting color) {
        return Component.translatable(key).withStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withColor(color));
    }

    private enum Phase { RINGING, ACTIVE }

    private static final class Session {
        final MinecraftServer server;
        final UUID caller;
        final UUID target;
        final String callerName;
        final String targetName;
        Phase phase = Phase.RINGING;
        long ringUntil;
        long startedAt;
        long lastCharge;
        long charged;

        Session(MinecraftServer server, UUID caller, UUID target, String callerName, String targetName) {
            this.server = server;
            this.caller = caller;
            this.target = target;
            this.callerName = callerName;
            this.targetName = targetName;
        }
    }
}