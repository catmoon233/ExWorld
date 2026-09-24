package net.exmo.lotm.phone;

import net.exmo.exworld.progress.PlayerProgressSystem;
import net.exmo.exworld.progress.PlayerResourceVault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Group chat, photo cards, transfers and red packets on the existing message list. */
final class PhoneChat {
    private PhoneChat() {}

    static String create(ServerPlayer player, String name, String members) {
        name = clip(name, 16);
        if (name.isBlank()) return "请填写群名";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Group group = new PhoneData.Group();
        group.id = "g" + (++data.groupSeq);
        group.name = name;
        group.owner = self.wechat;
        group.members.add(self.wechat);
        for (String part : members.split("[,， ]+")) {
            String wechat = part.trim();
            if (wechat.isBlank() || wechat.equalsIgnoreCase(self.wechat)) continue;
            PhoneData.Profile friend = data.byWechat(wechat);
            if (friend == null || self.friends.stream().noneMatch(item -> item.equalsIgnoreCase(friend.wechat))) return "只能拉好友进群：" + wechat;
            if (group.members.stream().noneMatch(item -> item.equalsIgnoreCase(friend.wechat))) group.members.add(friend.wechat);
        }
        if (group.members.size() < 2) return "至少再填一个好友微信名";
        data.groups.add(group);
        post(data, self.wechat, "g:" + group.id, self.wechat + " 创建了群聊", "text", "");
        tell(player, data, group, "已加入群聊 " + name);
        return "已创建 " + name;
    }

    static String say(ServerPlayer player, String to, String text) {
        if (to.isBlank() || text.isBlank()) return "请填写对象和内容";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (!canPost(data, self, to)) return to.startsWith("g:") ? "你不在这个群" : "请先添加好友";
        post(data, self.wechat, to, text, "text", "");
        PhoneData.Group room = group(data, to);
        if (room != null) tell(player, data, room, self.wechat + "：" + clip(text, 40));
        else notify(player, data.byWechat(to), self.wechat + "：" + clip(text, 40));
        return "已发送";
    }

    static String photo(ServerPlayer player, String to, int slot) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (!canPost(data, self, to)) return "不能发到这里";
        ItemStack photo = ExposurePhotos.photograph(player, slot);
        if (photo.isEmpty()) return "相册里没有这张照片";
        String label = photo.getHoverName().getString();
        give(player, data, to, photo);
        post(data, self.wechat, to, "[照片] " + clip(label, 40), "photo", clip(label, 40));
        PhoneData.Group room = group(data, to);
        if (room != null) tell(player, data, room, self.wechat + " 发了一张照片");
        else notify(player, data.byWechat(to), self.wechat + " 发了一张照片");
        return "已发送照片";
    }

    static String transfer(ServerPlayer player, String to, int amount) {
        if (amount <= 0 || amount > 1_000_000) return "金额无效";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (to.startsWith("g:")) return "群里请发红包，转账请填对方微信名";
        PhoneData.Profile target = data.byWechat(to);
        if (target == null || target.npc() || self.friends.stream().noneMatch(item -> item.equalsIgnoreCase(target.wechat))) return "只能转给好友";
        if (!take(player, amount)) return "金币不足";
        PhoneData.Packet packet = open(data, "transfer", self.wechat, target.wechat, amount, 1);
        post(data, self.wechat, target.wechat, "[转账] " + amount + " 金币", "transfer", packet.id);
        notify(player, target, self.wechat + " 转来 " + amount + " 金币，在聊天里领取");
        return "转账已发出，等待对方领取";
    }

    static String redpack(ServerPlayer player, String to, int amount, int shares) {
        if (!to.startsWith("g:")) return "红包只能发到群聊";
        if (amount <= 0 || shares <= 0 || shares > 20 || amount < shares) return "金额或份数无效";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Group group = group(data, to);
        if (group == null || !group.has(self.wechat)) return "你不在这个群";
        if (!take(player, amount)) return "金币不足";
        PhoneData.Packet packet = open(data, "redpack", self.wechat, to, amount, shares);
        post(data, self.wechat, to, "[红包] " + amount + " 金币 / " + shares + " 份", "redpack", packet.id);
        tell(player, data, group, self.wechat + " 发了红包");
        return "红包已发出";
    }

    static String claim(ServerPlayer player, String id) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Packet packet = packet(data, id);
        if (packet == null) return "没有这个转账或红包";
        if (packet.left <= 0 || packet.claimed.size() >= packet.shares) return "已经领完";
        if (packet.claimed.stream().anyMatch(name -> name.equalsIgnoreCase(self.wechat))) return "你已经领过";
        if ("transfer".equals(packet.kind) && !packet.target.equalsIgnoreCase(self.wechat)) return "这不是转给你的";
        if ("redpack".equals(packet.kind)) {
            PhoneData.Group group = group(data, packet.target);
            if (group == null || !group.has(self.wechat)) return "你不在这个群";
        }
        int share = packet.left / Math.max(1, packet.shares - packet.claimed.size());
        packet.claimed.add(self.wechat);
        packet.left -= share;
        data.touch();
        PlayerProgressSystem.vault().add(player.server, player.getUUID(), PlayerResourceVault.GOLD, share);
        String where = packet.target.startsWith("g:") ? packet.target : packet.from;
        post(data, self.wechat, where, self.wechat + " 领取了 " + share + " 金币", "text", "");
        if (packet.target.startsWith("g:")) tell(player, data, group(data, packet.target), self.wechat + " 领取了 " + share + " 金币");
        else notify(player, data.byWechat(packet.from), self.wechat + " 领取了 " + share + " 金币");
        return "领取了 " + share + " 金币";
    }

    static String voice(ServerPlayer player, String to) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Group group = group(data, to);
        if (group == null || !group.has(self.wechat)) return "请先选中群聊";
        return VoiceCalls.group(player, group.name, online(player, data, group));
    }

    private static PhoneData.Packet open(PhoneData data, String kind, String from, String target, int amount, int shares) {
        PhoneData.Packet packet = new PhoneData.Packet();
        packet.id = "p" + (++data.packetSeq);
        packet.kind = kind;
        packet.from = from;
        packet.target = target;
        packet.amount = amount;
        packet.shares = shares;
        packet.left = amount;
        data.packets.add(packet);
        data.touch();
        return packet;
    }

    private static void post(PhoneData data, String from, String to, String text, String kind, String extra) {
        data.messages.add(from + "\t" + to + "\t" + clip(text, 80) + "\t" + kind + "\t" + clip(extra, 64));
        while (data.messages.size() > 120) data.messages.remove(0);
        data.touch();
    }

    private static boolean canPost(PhoneData data, PhoneData.Profile self, String to) {
        if (to.startsWith("g:")) {
            PhoneData.Group group = group(data, to);
            return group != null && group.has(self.wechat);
        }
        PhoneData.Profile target = data.byWechat(to);
        return target != null && self.friends.stream().anyMatch(item -> item.equalsIgnoreCase(target.wechat));
    }

    private static void give(ServerPlayer actor, PhoneData data, String to, ItemStack photo) {
        for (ServerPlayer member : recipients(actor, data, to)) {
            ItemStack copy = photo.copy();
            copy.setCount(1);
            if (!member.getInventory().add(copy)) member.drop(copy, false);
        }
    }

    private static List<ServerPlayer> recipients(ServerPlayer actor, PhoneData data, String to) {
        List<ServerPlayer> players = new ArrayList<>();
        if (to.startsWith("g:")) {
            PhoneData.Group group = group(data, to);
            if (group == null) return players;
            for (String member : group.members) {
                PhoneData.Profile profile = data.byWechat(member);
                ServerPlayer online = online(actor, profile);
                if (online != null && online != actor) players.add(online);
            }
            return players;
        }
        ServerPlayer target = online(actor, data.byWechat(to));
        if (target != null) players.add(target);
        return players;
    }

    private static void tell(ServerPlayer actor, PhoneData data, PhoneData.Group group, String status) {
        if (group == null) return;
        for (String member : group.members) notify(actor, data.byWechat(member), status);
    }

    private static void notify(ServerPlayer actor, PhoneData.Profile target, String status) {
        ServerPlayer online = online(actor, target);
        if (online != null && online != actor) PhoneSystem.send(online, PhoneActions.snapshot(online, status));
    }

    private static ServerPlayer online(ServerPlayer actor, PhoneData.Profile profile) {
        if (profile == null || profile.npc()) return null;
        try {
            return actor.server.getPlayerList().getPlayer(java.util.UUID.fromString(profile.id));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<ServerPlayer> online(ServerPlayer actor, PhoneData data, PhoneData.Group group) {
        List<ServerPlayer> players = new ArrayList<>();
        for (String member : group.members) {
            ServerPlayer online = online(actor, data.byWechat(member));
            if (online != null) players.add(online);
        }
        return players;
    }

    private static PhoneData.Group group(PhoneData data, String to) {
        String id = to.startsWith("g:") ? to.substring(2) : to;
        for (PhoneData.Group group : data.groups) if (group.id.equals(id)) return group;
        return null;
    }

    private static PhoneData.Packet packet(PhoneData data, String id) {
        for (PhoneData.Packet packet : data.packets) if (packet.id.equals(id)) return packet;
        return null;
    }

    private static boolean take(ServerPlayer player, int amount) {
        return PlayerProgressSystem.vault().take(player.server, player.getUUID(), PlayerResourceVault.GOLD, amount);
    }

    private static String clip(String text, int max) {
        String value = text == null ? "" : text.replace('\t', ' ').replace('\n', ' ').trim();
        return value.length() <= max ? value : value.substring(0, max);
    }
}
