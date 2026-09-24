package net.exmo.exworld.client.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

/** Client copy of the wand's selected document geometry. */
public final class NpcToolView {
    public static String id = "";
    public static String name = "";
    public static int mode;
    public static ListTag places = new ListTag();
    public static ListTag routes = new ListTag();

    private NpcToolView() {}

    public static void install(CompoundTag tag) {
        if (tag == null || tag.getString("id").isBlank()) {
            clear();
            return;
        }
        id = tag.getString("id");
        name = tag.getString("name");
        mode = tag.getInt("mode");
        places = tag.getList("places", Tag.TAG_COMPOUND);
        routes = tag.getList("routes", Tag.TAG_COMPOUND);
    }

    public static void clear() {
        id = "";
        name = "";
        mode = 0;
        places = new ListTag();
        routes = new ListTag();
    }

    public static String nearest(Vec3 at, String dimension, double range) {
        String found = "";
        double best = range * range;
        for (int i = 0; i < places.size(); i++) {
            CompoundTag place = places.getCompound(i);
            if (!dimension.equals(place.getString("dim"))) continue;
            double dx = at.x - place.getDouble("x");
            double dy = at.y - place.getDouble("y");
            double dz = at.z - place.getDouble("z");
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= best) {
                best = distance;
                found = place.getString("id");
            }
        }
         return found;
     }
 
     public static int activePoints() {
         for (int i = 0; i < routes.size(); i++) {
             CompoundTag route = routes.getCompound(i);
             if (route.getBoolean("active")) return route.getList("points", Tag.TAG_COMPOUND).size();
         }
         return 0;
     }
 }
