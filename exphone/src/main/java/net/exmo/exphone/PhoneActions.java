package net.exmo.exphone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.exphone.api.PhoneEconomy;
import net.exmo.exphone.api.PhoneNpc;
import net.exmo.exphone.api.PhoneNpcs;
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
        if (action.startsWith("atm_")) {
            String saved = AtmActions.handle(player, action, number(body, "denom"), number(body, "count"), number(body, "amount"));
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(saved), true);
            PhoneSystem.send(player, AtmActions.openJson(player, saved));
            return;
        }
        if ("state".equals(action)) {
            PhoneProtocol.reset(player);
            PhoneSystem.send(player, snapshot(player, ""));
            return;
        }
        String request = text(body, "requestId");
        if (!PhoneProtocol.fresh(request, System.currentTimeMillis())) {
            PhoneProtocol.result(player, request, "EXPIRED_REQUEST", "操作已过期，请重新操作");
            return;
        }
        if (PhoneProtocol.replay(player, request)) return;
        if (PhoneStacks.heldPhone(player).isEmpty()) {
            PhoneProtocol.result(player, request, "NO_PHONE", "请手持手机"); return;
        }
        if (PhoneStacks.battery(PhoneStacks.heldPhone(player)) <= 0) {
            PhoneProtocol.result(player, request, "BATTERY_EMPTY", "电量不足"); return;
        }
        PhoneData.Profile selfProfile = PhoneData.get(player).ensure(player);
        if (wechat(action) && (selfProfile.number.isBlank() || !selfProfile.number.equals(selfProfile.boundNumber))) {
            PhoneProtocol.result(player, request, "WECHAT_UNBOUND", "请先用当前手机号绑定微信"); return;
        }
        if (wechat(action) && !PhoneWifi.online(player)) {
            PhoneProtocol.result(player, request, "OFFLINE", "未连接 WiFi，且没有可用移动数据流量"); return;
        }
        String target = text(body, "to");
        PhoneData.Profile targetProfile = PhoneData.get(player).profiles.get(target);
        if (targetProfile != null) body.addProperty("to", targetProfile.wechat);
        String status = switch (action) {
            case "chat" -> PhoneChat.say(player, text(body, "to"), text(body, "text"));
            case "group" -> PhoneChat.create(player, text(body, "text"), text(body, "to"));
            case "photo" -> PhoneChat.photo(player, text(body, "to"), number(body, "slot"));
            case "transfer" -> PhoneChat.transfer(player, text(body, "to"), number(body, "amount"));
            case "redpack" -> PhoneChat.redpack(player, text(body, "to"), number(body, "amount"), shares(body));
            case "claim" -> PhoneChat.claim(player, text(body, "id"));
            case "friend" -> friend(player, text(body, "to"));
            case "wechat_bind" -> bindWechat(player);
            case "contact_save" -> saveContact(player, text(body, "name"), text(body, "number"));
            case "contact_delete" -> deleteContact(player, text(body, "number"));
            case "accept" -> accept(player, text(body, "to"));
            case "reject" -> reject(player, text(body, "to"));
            case "rename" -> rename(player, text(body, "text"));
            case "moment" -> moment(player, text(body, "text"), text(body, "photo"));
            case "like" -> like(player, text(body, "id"));
            case "comment" -> comment(player, text(body, "id"), text(body, "text"));
            case "sms" -> sms(player, text(body, "to"), text(body, "text"));
            case "pay" -> pay(player, text(body, "to"), number(body, "amount"));
            case "npc" -> npc(player, text(body, "id"), text(body, "number"), text(body, "wechat"), text(body, "text"));
            case "camera" -> "已允许拍摄";
            case "image" -> PhoneChat.image(player, text(body, "to"), text(body, "photo"));
            case "audio" -> PhoneChat.audio(player, text(body, "to"), text(body, "audio"));
            case "call_accept" -> PhoneCalls.accept(player);
            case "call_reject" -> PhoneCalls.hangup(player);
            case "call" -> call(player, text(body, "to"));
            case "hangup" -> PhoneCalls.hangup(player);
            case "topup" -> PhoneWifi.topup(player, number(body, "amount"));
            case "data" -> PhoneWifi.buyData(player, number(body, "amount"));
            case "wifi" -> PhoneWifi.connect(player, text(body, "name"), text(body, "text"));
            case "unwifi" -> PhoneWifi.disconnect(player);
            case "setting" -> PhoneTools.setting(player, text(body, "name"), text(body, "text"));
            case "note" -> PhoneTools.note(player, text(body, "id"), text(body, "name"), text(body, "text"));
            case "note_delete" -> PhoneTools.deleteNote(player, text(body, "id"));
            default -> "未知操作";
        };
        // Existing gameplay modules return localized outcomes; only this compatibility adapter interprets them.
        boolean ok = status.startsWith("已") || status.startsWith("短信已") || status.startsWith("微信名已")
                || status.startsWith("转账已") || status.startsWith("红包已") || status.startsWith("正在呼叫")
                || status.startsWith("领取了") || status.startsWith("群语音已");
        if (ok) {
            PhoneStacks.useBattery(PhoneStacks.heldPhone(player));
            if (wechat(action)) PhoneWifi.gate(player);
        }
        PhoneProtocol.result(player, request, ok ? "OK" : "ACTION_REJECTED", status);
        PhoneSystem.send(player, snapshot(player, ""));
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
        if (to.isBlank()) return "请填写微信名或手机号";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (to.equalsIgnoreCase(self.wechat) || to.equals(self.number)) return "不能添加自己";
        PhoneData.Profile target = java.util.Optional.ofNullable(data.byWechat(to)).orElseGet(() -> data.byNumber(to));
        if (target == null) return "没有这个微信或手机号";
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

    private static String bindWechat(ServerPlayer player) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (self.number.isBlank()) return "请先安装 SIM 卡";
        if (self.number.equals(self.boundNumber)) return "已绑定当前手机号";
        self.boundNumber = self.number;
        data.touch();
        return "已绑定手机号 " + self.number;
    }

    private static String saveContact(ServerPlayer player, String name, String number) {
        String digits = number.replaceAll("\\D", "");
        if (digits.length() < 3 || digits.length() > 16) return "号码格式不正确";
        String label = clip(name, 24);
        if (label.isBlank()) return "请填写联系人名称";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (self.phoneContacts.size() >= 200 && !self.phoneContacts.containsKey(digits)) return "通讯录已满";
        self.phoneContacts.put(digits, label);
        data.touch();
        return "已保存联系人";
    }

    private static String deleteContact(ServerPlayer player, String number) {
        PhoneData data = PhoneData.get(player);
        if (data.ensure(player).phoneContacts.remove(number) == null) return "找不到联系人";
        data.touch();
        return "已删除联系人";
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

    private static String moment(ServerPlayer player, String text, String photo) {
        if (text.isBlank() && photo.isBlank()) return "朋友圈内容不能为空";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (!photo.isBlank() && !self.id.equals(data.photoOwners.get(photo))) return "照片不存在或不属于你";
        addMoment(data, self.wechat, text);
        data.moments.getLast().photo = photo;
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
        if (!PhoneEconomy.transfer(player.server, player.getUUID(), target.getUUID(), PhoneEconomy.GOLD, amount)) return "金币不足";
        PhoneSystem.send(target, snapshot(target, name(player) + " 转来 " + amount + " 金币"));
        return "已转账 " + amount + " 金币";
    }

    private static String npc(ServerPlayer player, String id, String number, String wechat, String momentText) {
        if (!editor(player)) return "需要创造模式或管理员";
        if (id.isBlank()) return "请选择 NPC";
        PhoneNpc document = PhoneNpcs.find(player.server, id);
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
        return "已保存 " + document.name();
    }

    private static String call(ServerPlayer player, String to) {
        PhoneData data = PhoneData.get(player);
        if (data.ensure(player).number.isBlank()) return "请先安装 SIM 卡";
        ServerPlayer target = resolvePlayer(player, to);
        if (target == null) return "对方不在线";
        String status = PhoneCalls.call(player, target.getGameProfile().getName());
        boolean live = status.startsWith("正在呼叫");
        PhoneTools.logCall(player, target.getGameProfile().getName(), live ? "out" : "miss");
        PhoneTools.logCall(target, name(player), live ? "in" : "miss");
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
        root.addProperty("wechatBound", !self.number.isBlank() && self.number.equals(self.boundNumber));
        root.addProperty("gold", PhoneEconomy.balance(player.server, player.getUUID(), PhoneEconomy.GOLD));
        root.addProperty("editor", editor(player));
        root.addProperty("camera", true);
        root.addProperty("selfId", self.id);
        root.add("callState", PhoneCalls.state(player));
        root.addProperty("voice", PhoneCalls.installed());
        root.addProperty("online", PhoneWifi.online(player));
        root.addProperty("wifi", PhoneWifi.current(player));
         root.addProperty("credit", PhoneWifi.credit(player));
         root.addProperty("battery", PhoneStacks.battery(PhoneStacks.heldPhone(player)));
        root.addProperty("data", PhoneWifi.data(player));
        root.addProperty("theme", self.theme);
        root.addProperty("font", self.font);
        root.add("notes", PhoneTools.notes(self));
        root.add("calls", PhoneTools.calls(self));
        root.add("wifis", PhoneWifi.nearby(player));
        root.addProperty("call", PhoneCalls.partner(player));
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
            card.addProperty("id", profile == null ? "" : profile.id);
            card.addProperty("number", profile == null ? "" : profile.number);
            card.addProperty("kind", profile != null && profile.npc() ? "npc" : "player");
            book.add(card);
        }
        root.add("friends", friends);
        root.add("book", book);
        JsonArray phoneContacts = new JsonArray();
        self.phoneContacts.forEach((number, label) -> {
            JsonObject contact = new JsonObject(); contact.addProperty("number", number); contact.addProperty("name", label); phoneContacts.add(contact);
        });
        root.add("phoneContacts", phoneContacts);
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
        for (int i = data.conversations.size() - 1; i >= 0 && shown < 24; i--) {
            PhoneMessage line = data.conversations.get(i);
            if (!line.visible(data, self)) continue;
            JsonObject message = new JsonObject();
            message.addProperty("id", line.id());
            message.addProperty("conversation", line.conversation(self));
            message.addProperty("fromId", line.fromId());
            message.addProperty("from", line.fromName(data));
            message.addProperty("to", line.toName(data));
            message.addProperty("text", line.text());
            message.addProperty("kind", line.kind());
            message.addProperty("extra", line.extra());
            message.addProperty("time", line.time());
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
            item.addProperty("photo", moment.photo);
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
        int shown = 0;
        for (PhoneNpc document : PhoneNpcs.list(player.server)) {
            if (shown++ >= 40) break;
            PhoneData.Profile profile = data.profiles.get("npc:" + document.id());
            JsonObject item = new JsonObject();
            item.addProperty("id", document.id());
            item.addProperty("name", document.name());
            item.addProperty("number", profile == null ? "" : profile.number);
            item.addProperty("wechat", profile == null ? "" : profile.wechat);
            array.add(item);
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
        if (online != null && online != actor) {
            PhoneSystem.send(online, snapshot(online, status));
            PhoneStacks.alert(online, status);
        }
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
            case "chat", "group", "photo", "image", "audio", "transfer", "redpack", "claim",
                    "friend", "accept", "reject", "rename", "moment", "like", "comment" -> true;
            default -> false;
        };
    }

    private static PhoneNpc npcDocument(ServerPlayer player, String id) {
        return PhoneNpcs.find(player.server, id);
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
        return clip(text, 2000);
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
