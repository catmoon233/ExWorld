package net.exmo.lotm.phone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

/** Phone settings and the memo book. Calculator, timer and converter stay on the page. */
final class PhoneTools {
    private PhoneTools() {}

    static String setting(ServerPlayer player, String key, String value) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if ("theme".equals(key)) {
            if (!"light".equals(value) && !"dark".equals(value)) return "主题无效";
            self.theme = value;
        } else if ("font".equals(key)) {
            if (!"13".equals(value) && !"14".equals(value) && !"16".equals(value)) return "字号无效";
            self.font = value;
        } else {
            return "没有这项设置";
        }
        data.touch();
        return "设置已保存";
    }

    static String note(ServerPlayer player, String id, String title, String text) {
        title = clip(title, 24);
        text = clip(text, 180);
        if (title.isBlank() && text.isBlank()) return "请填写标题或内容";
        if (title.isBlank()) title = "备忘";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        String line = title + "\t" + text;
        if (id == null || id.isBlank()) {
            id = "n" + (++self.noteSeq);
            self.notes.add(id + "\t" + line);
        } else {
            int index = index(self, id);
            if (index < 0) return "没有这条记事";
            self.notes.set(index, id + "\t" + line);
        }
        while (self.notes.size() > 40) self.notes.remove(0);
        data.touch();
        return "已保存记事";
    }

    static String deleteNote(ServerPlayer player, String id) {
        if (id == null || id.isBlank()) return "请选择记事";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        int index = index(self, id);
        if (index < 0) return "没有这条记事";
        self.notes.remove(index);
        data.touch();
        return "已删除";
    }

    static JsonArray notes(PhoneData.Profile self) {
        JsonArray array = new JsonArray();
        for (String line : self.notes) {
            String[] parts = line.split("\t", 3);
            JsonObject item = new JsonObject();
            item.addProperty("id", parts[0]);
            item.addProperty("title", parts.length > 1 ? parts[1] : "");
            item.addProperty("text", parts.length > 2 ? parts[2] : "");
            array.add(item);
        }
        return array;
    }

    private static int index(PhoneData.Profile self, String id) {
        for (int i = 0; i < self.notes.size(); i++) {
            String line = self.notes.get(i);
            int cut = line.indexOf('\t');
            String found = cut < 0 ? line : line.substring(0, cut);
            if (found.equals(id)) return i;
        }
        return -1;
    }

    private static String clip(String text, int max) {
        String value = text == null ? "" : text.replace('\t', ' ').replace('\n', ' ').trim();
        return value.length() <= max ? value : value.substring(0, max);
    }
}
