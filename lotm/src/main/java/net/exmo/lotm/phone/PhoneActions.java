package net.exmo.lotm.phone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.progress.PlayerProgressSystem;
import net.exmo.exworld.progress.PlayerResourceVault;
import net.minecraft.server.level.ServerPlayer;

final class PhoneActions {
    private PhoneActions() {}

    static void handle(ServerPlayer player, String json) {
        JsonObject body = parse(json);
        String action = text(body, "action");
        if ("map_save".equals(action) || "map_text".equals(action) || "map_delete".equals(action)) {
            map(player, body, "map_save".equals(action));
            return;
        }
        if ("wifi_save".equals(action)) {
            String saved = PhoneWifi.save(player, number(body, "x"), number(body, "y"), number(body, "z"), text(body, "name"), text(body, "text"), number(body, "range"));
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(saved), true);
            return;
        }
         if (PhoneStacks.heldPhone(player).isEmpty()) {
             PhoneSystem.send(player, snapshot(player, "请手持手机"));
             return;
         }
         if (!"state".equals(action) && !PhoneStacks.useBattery(PhoneStacks.heldPhone(player))) {
             PhoneSystem.send(player, snapshot(player, "电量不足"));
             return;
         }
        if (wechat(action)) {
            String blocked = PhoneWifi.gate(player);
            if (!blocked.isBlank()) {
                PhoneSystem.send(player, snapshot(player, blocked));
                return;
            }
        }
        String status = switch (action) {
            case "chat" -> PhoneChat.say(player, text(body, "to"), text(body, "text"));
            case "group" -> PhoneChat.create(player, text(body, "text"), text(body, "to"));
            case "photo" -> PhoneChat.photo(player, text(body, "to"), number(body, "slot"));
            case "transfer" -> PhoneChat.transfer(player, text(body, "to"), number(body, "amount"));
            case "redpack" -> PhoneChat.redpack(player, text(body, "to"), number(body, "amount"), shares(body));
            case "claim" -> PhoneChat.claim(player, text(body, "id"));
            case "voice" -> PhoneChat.voice(player, text(body, "to"));
            case "friend" -> friend(player, text(body, "to"));
            case "accept" -> accept(player, text(body, "to"));
            case "reject" -> reject(player, text(body, "to"));
            case "rename" -> rename(player, text(body, "text"));
            case "moment" -> moment(player, text(body, "text"));
            case "like" -> like(player, text(body, "id"));
            case "comment" -> comment(player, text(body, "id"), text(body, "text"));
            case "sms" -> sms(player, text(body, "to"), text(body, "text"));
            case "pay" -> pay(player, text(body, "to"), number(body, "amount"));
            case "npc" -> npc(player, text(body, "id"), text(body, "number"), text(body, "wechat"), text(body, "text"));
            case "camera" -> ExposurePhotos.open(player);
            case "call" -> call(player, text(body, "to"));
            case "hangup" -> VoiceCalls.hangup(player);
            case "topup" -> PhoneWifi.topup(player, number(body, "amount"));
            case "data" -> PhoneWifi.buyData(player, number(body, "amount"));
            case "wifi" -> PhoneWifi.connect(player, text(body, "name"), text(body, "text"));
            case "unwifi" -> PhoneWifi.disconnect(player);
            case "setting" -> PhoneTools.setting(player, text(body, "name"), text(body, "text"));
            case "note" -> PhoneTools.note(player, text(body, "id"), text(body, "name"), text(body, "text"));
            case "note_delete" -> PhoneTools.deleteNote(player, text(body, "id"));
            default -> "";
        };
        PhoneSystem.send(player, snapshot(player, status));
    }

    private static void map(ServerPlayer player, JsonObject body, boolean useBox) {
        PhoneMap map = PhoneMap.get(player);
        String status = "map_delete".equals(text(body, "action"))
                ? map.delete(player, text(body, "id"))
                : map.save(player, text(body, "id"), text(body, "name"), text(body, "tag"), text(body, "info"), text(body, "kind"),
                useBox && body.has("useBox") && body.get("useBox").getAsBoolean(), number(body, "x1"), number(body, "y1"), number(body, "z1"),
                number(body, "x2"), number(body, "y2"), number(body, "z2"), text(body, "dim"));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(status), true);
        net.minecraft.world.item.ItemStack wand = PhoneStacks.heldPhone(player);
        if (!(player.getMainHandItem().getItem() instanceof MapWandItem)) {
            if (player.getOffhandItem().getItem() instanceof MapWandItem) wand = player.getOffhandItem();
            else wand = player.getMainHandItem().getItem() instanceof MapWandItem ? player.getMainHandItem() : wand;
        } else wand = player.getMainHandItem();
        if (wand.getItem() instanceof MapWandItem) PhoneSystem.send(player, map.editorJson(wand));
    }

    private static String friend(ServerPlayer player, String to) {
        if (to.isBlank()) return "请填写微信名";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (to.equalsIgnoreCase(self.wechat)) return "不能添加自己";
        PhoneData.Profile target = data.byWechat(to);
        if (target == null) return "没有这个微信";
        if (self.friends.stream().anyMatch(name -> name.equalsIgnoreCase(target.wechat))) return "已经是好友";
        if (target.npc()) {
            link(self, target);
            data.touch();
            return "已添加 " + target.wechat;
        }
        if (target.requests.stream().noneMatch(name -> name.equalsIgnoreCase(self.wechat))) target.requests.add(self.wechat);
        data.touch();
        notifyOwner(player, target, self.wechat + " 请求加好友");
        return "已发送好友申请";
    }

    private static String accept(ServerPlayer player, String to) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Profile target = data.byWechat(to);
        if (target == null || self.requests.stream().noneMatch(name -> name.equalsIgnoreCase(to))) return "没有这条申请";
        self.requests.removeIf(name -> name.equalsIgnoreCase(to));
        link(self, target);
        data.touch();
        notifyOwner(player, target, self.wechat + " 已通过好友申请");
        return "已添加 " + target.wechat;
    }

    private static String reject(ServerPlayer player, String to) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (!self.requests.removeIf(name -> name.equalsIgnoreCase(to))) return "没有这条申请";
        data.touch();
        return "已拒绝";
    }

    private static String rename(ServerPlayer player, String text) {
        String name = clip(text, 16);
        if (name.isBlank()) return "微信名不能为空";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Profile taken = data.byWechat(name);
        if (taken != null && taken != self) return "微信名已被使用";
        String old = self.wechat;
        self.wechat = name;
        for (PhoneData.Profile profile : data.profiles.values()) {
            replace(profile.friends, old, name);
            replace(profile.requests, old, name);
        }
        for (PhoneData.Group group : data.groups) {
            replace(group.members, old, name);
            if (group.owner.equalsIgnoreCase(old)) group.owner = name;
        }
        for (PhoneData.Packet packet : data.packets) {
            if (packet.from.equalsIgnoreCase(old)) packet.from = name;
            if (!packet.target.startsWith("g:") && packet.target.equalsIgnoreCase(old)) packet.target = name;
            replace(packet.claimed, old, name);
        }
        data.touch();
        return "微信名已改为 " + name;
    }

    private static String moment(ServerPlayer player, String text) {
        if (text.isBlank()) return "朋友圈内容不能为空";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        addMoment(data, self.wechat, text);
        return "已发布朋友圈";
    }

    private static String like(ServerPlayer player, String id) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Moment moment = moment(data, id);
        if (moment == null || !visible(self, moment, data)) return "看不到这条朋友圈";
        if (!moment.likes.remove(self.wechat)) moment.likes.add(self.wechat);
        data.touch();
        return "已更新点赞";
    }

    private static String comment(ServerPlayer player, String id, String text) {
        if (text.isBlank()) return "评论不能为空";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        PhoneData.Moment moment = moment(data, id);
        if (moment == null || !visible(self, moment, data)) return "看不到这条朋友圈";
        moment.comments.add(self.wechat + "\t" + clip(text));
        while (moment.comments.size() > 20) moment.comments.remove(0);
        data.touch();
        return "已评论";
    }

    private static String sms(ServerPlayer player, String to, String text) {
        if (to.isBlank() || text.isBlank()) return "请填写号码和内容";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (self.number.isBlank()) return "请先安装 SIM 卡";
        if (data.byNumber(to) == null) return "号码不存在";
        PhoneData.Sms line = new PhoneData.Sms();
        line.from = self.number;
        line.to = to;
        line.text = clip(text);
        data.sms.add(line);
        trimSms(data);
        data.touch();
        PhoneData.Profile target = data.byNumber(to);
        notifyOwner(player, target, "短信来自 " + self.number);
        return "短信已发送";
    }

    private static String pay(ServerPlayer player, String to, int amount) {
        if (amount <= 0 || amount > 1_000_000) return "金额无效";
        ServerPlayer target = resolvePlayer(player, to);
        if (target == null) return "对方不在线或没有钱包";
        if (target == player) return "不能转给自己";
        var vault = PlayerProgressSystem.vault();
        if (!vault.transfer(player.server, player.getUUID(), target.getUUID(), PlayerResourceVault.GOLD, amount)) return "金币不足";
        PhoneSystem.send(target, snapshot(target, name(player) + " 转来 " + amount + " 金币"));
        return "已转账 " + amount + " 金币";
    }

    private static String npc(ServerPlayer player, String id, String number, String wechat, String momentText) {
        if (!editor(player)) return "需要创造模式或管理员";
        if (id.isBlank()) return "请选择 NPC";
        NpcDocument document = npcDocument(player, id);
        if (document == null) return "NPC 不存在";
        String digits = number.replaceAll("\\D", "");
        if (!digits.isBlank() && (digits.length() < 3 || digits.length() > 12)) return "号码需要 3 到 12 位";
        String card = clip(wechat, 16);
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile takenNumber = digits.isBlank() ? null : data.byNumber(digits);
        if (takenNumber != null && !takenNumber.id.equals("npc:" + id)) return "号码已被占用";
        PhoneData.Profile takenName = card.isBlank() ? null : data.byWechat(card);
        if (takenName != null && !takenName.id.equals("npc:" + id)) return "微信名已被使用";
        PhoneData.Profile profile = data.npc(id);
        profile.number = digits;
        profile.wechat = card;
        data.touch();
        if (!momentText.isBlank()) {
            if (card.isBlank()) return "发朋友圈前请先填写微信名";
            addMoment(data, card, momentText);
        }
        return "已保存 " + document.displayName();
    }

    private static String call(ServerPlayer player, String to) {
        PhoneData data = PhoneData.get(player);
        if (data.ensure(player).number.isBlank()) return "请先安装 SIM 卡";
        ServerPlayer target = resolvePlayer(player, to);
        if (target == null) return "对方不在线";
        String status = VoiceCalls.call(player, target.getGameProfile().getName());
        if (status.startsWith("正在呼叫")) PhoneSystem.send(target, snapshot(target, name(player) + " 来电"));
        return status;
    }

    static String snapshot(ServerPlayer player, String status) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        JsonObject root = new JsonObject();
        root.addProperty("status", status);
        root.addProperty("self", name(player));
        root.addProperty("wechat", self.wechat);
        root.addProperty("number", self.number);
        root.addProperty("gold", PlayerProgressSystem.vault().balance(player.server, player.getUUID(), PlayerResourceVault.GOLD));
        root.addProperty("editor", editor(player));
        root.addProperty("camera", ExposurePhotos.installed());
        root.addProperty("voice", VoiceCalls.installed());
        root.addProperty("online", PhoneWifi.online(player));
        root.addProperty("wifi", PhoneWifi.current(player));
         root.addProperty("credit", PhoneWifi.credit(player));
         root.addProperty("battery", PhoneStacks.battery(PhoneStacks.heldPhone(player)));
        root.addProperty("data", PhoneWifi.data(player));
        root.addProperty("theme", self.theme);
        root.addProperty("font", self.font);
        root.add("notes", PhoneTools.notes(self));
        root.add("wifis", PhoneWifi.nearby(player));
        root.addProperty("call", VoiceCalls.partner(player));
        root.add("map", PhoneMap.get(player).list());
        root.addProperty("px", player.getX());
        root.addProperty("pz", player.getZ());
        root.addProperty("dim", player.serverLevel().dimension().location().toString());
        JsonArray players = new JsonArray();
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (online != player) players.add(online.getGameProfile().getName());
        }
        root.add("players", players);
        JsonArray friends = new JsonArray();
        JsonArray book = new JsonArray();
        for (String friend : self.friends) {
            friends.add(friend);
            PhoneData.Profile profile = data.byWechat(friend);
            JsonObject card = new JsonObject();
            card.addProperty("name", friend);
            card.addProperty("number", profile == null ? "" : profile.number);
            card.addProperty("kind", profile != null && profile.npc() ? "npc" : "player");
            book.add(card);
        }
        root.add("friends", friends);
        root.add("book", book);
        JsonArray requests = new JsonArray();
        self.requests.forEach(requests::add);
        root.add("requests", requests);
        root.add("messages", messages(data, self));
        root.add("sms", sms(data, self.number));
        root.add("moments", moments(data, self));
        root.add("npcs", npcs(player, data));
        root.add("groups", groups(data, self));
        root.add("album", ExposurePhotos.album(player));
        root.add("packets", packets(data, self));
        return root.toString();
    }

    private static JsonArray messages(PhoneData data, PhoneData.Profile self) {
        JsonArray array = new JsonArray();
        int shown = 0;
        for (int i = data.messages.size() - 1; i >= 0 && shown < 60; i--) {
            String[] parts = data.messages.get(i).split("\t", 5);
            if (parts.length < 3 || !sees(data, self, parts[0], parts[1])) continue;
            JsonObject message = new JsonObject();
            message.addProperty("from", parts[0]);
            message.addProperty("to", parts[1]);
            message.addProperty("text", parts[2]);
            message.addProperty("kind", parts.length > 3 && !parts[3].isBlank() ? parts[3] : "text");
            message.addProperty("extra", parts.length > 4 ? parts[4] : "");
            array.add(message);
            shown++;
        }
        return array;
    }

    private static boolean sees(PhoneData data, PhoneData.Profile self, String from, String to) {
        if (from.equals(self.wechat) || to.equals(self.wechat)) return true;
        PhoneData.Group group = groupOf(data, to);
        return group != null && group.has(self.wechat);
    }

    private static PhoneData.Group groupOf(PhoneData data, String to) {
        if (to == null || !to.startsWith("g:") || to.length() < 3) return null;
        String id = to.substring(2);
        for (PhoneData.Group group : data.groups) if (group.id.equals(id)) return group;
        return null;
    }

    private static JsonArray groups(PhoneData data, PhoneData.Profile self) {
        JsonArray array = new JsonArray();
        for (PhoneData.Group group : data.groups) {
            if (!group.has(self.wechat)) continue;
            JsonObject item = new JsonObject();
            item.addProperty("id", group.id);
            item.addProperty("name", group.name);
            JsonArray members = new JsonArray();
            group.members.forEach(members::add);
            item.add("members", members);
            array.add(item);
        }
        return array;
    }

    private static JsonArray packets(PhoneData data, PhoneData.Profile self) {
        JsonArray array = new JsonArray();
        for (PhoneData.Packet packet : data.packets) {
            PhoneData.Group group = groupOf(data, packet.target);
            boolean involved = packet.from.equalsIgnoreCase(self.wechat) || packet.target.equalsIgnoreCase(self.wechat) || (group != null && group.has(self.wechat));
            if (!involved) continue;
            JsonObject item = new JsonObject();
            item.addProperty("id", packet.id);
            item.addProperty("kind", packet.kind);
            item.addProperty("from", packet.from);
            item.addProperty("target", packet.target);
            item.addProperty("amount", packet.amount);
            item.addProperty("shares", packet.shares);
            item.addProperty("left", packet.left);
            item.addProperty("claimed", packet.claimed.stream().anyMatch(name -> name.equalsIgnoreCase(self.wechat)));
            array.add(item);
        }
        return array;
    }

    private static JsonArray sms(PhoneData data, String number) {
        JsonArray array = new JsonArray();
        if (number.isBlank()) return array;
        int shown = 0;
        for (int i = data.sms.size() - 1; i >= 0 && shown < 40; i--) {
            PhoneData.Sms line = data.sms.get(i);
            if (!number.equals(line.from) && !number.equals(line.to)) continue;
            JsonObject item = new JsonObject();
            item.addProperty("from", line.from);
            item.addProperty("to", line.to);
            item.addProperty("text", line.text);
            array.add(item);
            shown++;
        }
        return array;
    }

    private static JsonArray moments(PhoneData data, PhoneData.Profile self) {
        JsonArray array = new JsonArray();
        int shown = 0;
        for (int i = data.moments.size() - 1; i >= 0 && shown < 20; i--) {
            PhoneData.Moment moment = data.moments.get(i);
            if (!visible(self, moment, data)) continue;
            JsonObject item = new JsonObject();
            item.addProperty("id", moment.id);
            item.addProperty("author", moment.author);
            item.addProperty("text", moment.text);
            item.addProperty("likes", moment.likes.size());
            item.addProperty("liked", moment.likes.contains(self.wechat));
            JsonArray comments = new JsonArray();
            for (String comment : moment.comments) {
                String[] parts = comment.split("\t", 2);
                JsonObject line = new JsonObject();
                line.addProperty("from", parts[0]);
                line.addProperty("text", parts.length > 1 ? parts[1] : "");
                comments.add(line);
            }
            item.add("comments", comments);
            array.add(item);
            shown++;
        }
        return array;
    }

    private static JsonArray npcs(ServerPlayer player, PhoneData data) {
        JsonArray array = new JsonArray();
        if (!editor(player)) return array;
        try {
            int shown = 0;
            for (NpcDocument document : NpcCatalog.get(player.server).documents()) {
                if (shown++ >= 40) break;
                PhoneData.Profile profile = data.profiles.get("npc:" + document.id());
                JsonObject item = new JsonObject();
                item.addProperty("id", document.id());
                item.addProperty("name", document.displayName());
                item.addProperty("number", profile == null ? "" : profile.number);
                item.addProperty("wechat", profile == null ? "" : profile.wechat);
                array.add(item);
            }
        } catch (RuntimeException ignored) {
        }
        return array;
    }

    private static void addMoment(PhoneData data, String author, String text) {
        PhoneData.Moment moment = new PhoneData.Moment();
        moment.id = Long.toString(++data.momentSeq);
        moment.author = author;
        moment.text = clip(text);
        data.moments.add(moment);
        while (data.moments.size() > 60) data.moments.remove(0);
        data.touch();
    }

    private static PhoneData.Moment moment(PhoneData data, String id) {
        for (PhoneData.Moment moment : data.moments) {
            if (moment.id.equals(id)) return moment;
        }
        return null;
    }

    private static boolean visible(PhoneData.Profile self, PhoneData.Moment moment, PhoneData data) {
        if (moment.author.equalsIgnoreCase(self.wechat)) return true;
        return self.friends.stream().anyMatch(name -> name.equalsIgnoreCase(moment.author));
    }

    private static void link(PhoneData.Profile left, PhoneData.Profile right) {
        if (left.friends.stream().noneMatch(name -> name.equalsIgnoreCase(right.wechat))) left.friends.add(right.wechat);
        if (!right.wechat.isBlank() && right.friends.stream().noneMatch(name -> name.equalsIgnoreCase(left.wechat))) right.friends.add(left.wechat);
    }

    private static void replace(java.util.List<String> values, String oldName, String next) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(oldName)) values.set(i, next);
        }
    }

    private static void notifyOwner(ServerPlayer actor, PhoneData.Profile target, String status) {
        if (target == null || target.npc()) return;
        ServerPlayer online = actor.server.getPlayerList().getPlayer(java.util.UUID.fromString(target.id));
        if (online != null && online != actor) PhoneSystem.send(online, snapshot(online, status));
    }

    private static ServerPlayer resolvePlayer(ServerPlayer player, String token) {
        if (token == null || token.isBlank()) return null;
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (online.getGameProfile().getName().equalsIgnoreCase(token)) return online;
        }
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile profile = data.byNumber(token);
        if (profile == null) profile = data.byWechat(token);
        if (profile == null || profile.npc()) return null;
        try {
            return player.server.getPlayerList().getPlayer(java.util.UUID.fromString(profile.id));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean wechat(String action) {
        return switch (action) {
            case "chat", "group", "photo", "transfer", "redpack", "claim", "voice",
                    "friend", "accept", "reject", "rename", "moment", "like", "comment" -> true;
            default -> false;
        };
    }

    private static NpcDocument npcDocument(ServerPlayer player, String id) {
        try {
            return NpcCatalog.get(player.server).document(id).orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean editor(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    private static void trim(java.util.List<String> values, int max) {
        while (values.size() > max) values.remove(0);
    }

    private static void trimSms(PhoneData data) {
        while (data.sms.size() > 80) data.sms.remove(0);
    }

    private static String clip(String text) {
        return clip(text, 80);
    }

    private static String clip(String text, int max) {
        String value = text == null ? "" : text.replace('\t', ' ').replace('\n', ' ').trim();
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    private static JsonObject parse(String json) {
        try {
            return JsonParser.parseString(json == null ? "{}" : json).getAsJsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private static String text(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString().trim() : "";
    }

    private static int shares(JsonObject body) {
        int value = number(body, "shares");
        return value <= 0 ? 1 : value;
    }

    private static int number(JsonObject body, String key) {
        try {
            return body.has(key) ? body.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
