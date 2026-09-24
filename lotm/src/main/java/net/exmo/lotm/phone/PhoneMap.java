package net.exmo.lotm.phone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Hand-authored places and roads shown on the phone map. */
public final class PhoneMap extends SavedData {
    public static final String ID = "lotm_phone_map";
    public static final Factory<PhoneMap> FACTORY = new Factory<>(PhoneMap::new, PhoneMap::load);
    private final List<Place> places = new ArrayList<>();
    private long seq;

    public static PhoneMap get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public String save(ServerPlayer player, String id, String name, String tag, String info, String kind,
                       boolean useBox, int x1, int y1, int z1, int x2, int y2, int z2, String dimension) {
        if (!editor(player)) return "需要创造模式或管理员";
        name = clip(name, 24);
        if (name.isBlank()) return "请填写地点名称";
        Place place = find(id);
        if (place == null) {
            if (!useBox) return "请先用地图笔框选方块";
            place = new Place();
            place.id = Long.toString(++seq);
            places.add(place);
        }
        if (useBox) {
            if (span(x1, x2) > 256 || span(z1, z2) > 256) return "框选过大";
            place.x1 = Math.min(x1, x2);
            place.y1 = Math.min(y1, y2);
            place.z1 = Math.min(z1, z2);
            place.x2 = Math.max(x1, x2);
            place.y2 = Math.max(y1, y2);
            place.z2 = Math.max(z1, z2);
            place.dimension = dimension == null ? "" : dimension;
        }
        place.name = name;
        place.tag = clip(tag, 8);
        place.info = clip(info, 120);
        place.kind = "road".equals(kind) ? "road" : "place";
        while (places.size() > 80) places.remove(0);
        setDirty();
        return "已放入地图：" + name;
    }

    public String delete(ServerPlayer player, String id) {
        if (!editor(player)) return "需要创造模式或管理员";
        if (!places.removeIf(place -> place.id.equals(id))) return "地点不存在";
        setDirty();
        return "已从地图移除";
    }

    public JsonArray list() {
        JsonArray array = new JsonArray();
        for (Place place : places) array.add(place.json());
        return array;
    }

    public String editorJson(ItemStack wand) {
        JsonObject root = new JsonObject();
        root.addProperty("openMap", true);
        CompoundTag tag = wand.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        boolean ready = tag.getBoolean("hasA") && tag.getBoolean("hasB");
        root.addProperty("ready", ready);
        root.addProperty("kind", tag.getInt("kind") == 1 ? "road" : "place");
        root.addProperty("dim", tag.getString("dim"));
        if (tag.getBoolean("hasA")) {
            root.addProperty("x1", tag.getInt("ax"));
            root.addProperty("y1", tag.getInt("ay"));
            root.addProperty("z1", tag.getInt("az"));
        }
        if (ready) {
            root.addProperty("x2", tag.getInt("bx"));
            root.addProperty("y2", tag.getInt("by"));
            root.addProperty("z2", tag.getInt("bz"));
        }
        root.add("places", list());
        return root.toString();
    }

    private Place find(String id) {
        if (id == null || id.isBlank()) return null;
        for (Place place : places) if (place.id.equals(id)) return place;
        return null;
    }

    private static int span(int a, int b) {
        return Math.abs(a - b) + 1;
    }

    private static String clip(String value, int max) {
        String text = value == null ? "" : value.replace('\n', ' ').trim();
        return text.length() <= max ? text : text.substring(0, max);
    }

    static boolean editor(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    private static PhoneMap load(CompoundTag tag, HolderLookup.Provider registries) {
        PhoneMap map = new PhoneMap();
        map.seq = tag.getLong("seq");
        ListTag list = tag.getList("places", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) map.places.add(Place.load(list.getCompound(i)));
        return map;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("seq", seq);
        ListTag list = new ListTag();
        for (Place place : places) list.add(place.save());
        tag.put("places", list);
        return tag;
    }

    static final class Place {
        String id = "";
        String name = "";
        String tag = "";
        String info = "";
        String kind = "place";
        String dimension = "";
        int x1, y1, z1, x2, y2, z2;

        JsonObject json() {
            JsonObject item = new JsonObject();
            item.addProperty("id", id);
            item.addProperty("name", name);
            item.addProperty("tag", tag);
            item.addProperty("info", info);
            item.addProperty("kind", kind);
            item.addProperty("dim", dimension);
            item.addProperty("x1", x1);
            item.addProperty("y1", y1);
            item.addProperty("z1", z1);
            item.addProperty("x2", x2);
            item.addProperty("y2", y2);
            item.addProperty("z2", z2);
            return item;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id);
            tag.putString("name", name);
            tag.putString("tag", this.tag);
            tag.putString("info", info);
            tag.putString("kind", kind);
            tag.putString("dim", dimension);
            tag.putInt("x1", x1);
            tag.putInt("y1", y1);
            tag.putInt("z1", z1);
            tag.putInt("x2", x2);
            tag.putInt("y2", y2);
            tag.putInt("z2", z2);
            return tag;
        }

        static Place load(CompoundTag tag) {
            Place place = new Place();
            place.id = tag.getString("id");
            place.name = tag.getString("name");
            place.tag = tag.getString("tag");
            place.info = tag.getString("info");
            place.kind = tag.getString("kind");
            place.dimension = tag.getString("dim");
            place.x1 = tag.getInt("x1");
            place.y1 = tag.getInt("y1");
            place.z1 = tag.getInt("z1");
            place.x2 = tag.getInt("x2");
            place.y2 = tag.getInt("y2");
            place.z2 = tag.getInt("z2");
            return place;
        }
    }
}
