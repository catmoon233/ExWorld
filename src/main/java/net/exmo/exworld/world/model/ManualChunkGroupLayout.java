package net.exmo.exworld.world.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deep manual-group module. Its single interface turns an editor draft into a complete, connected partition of
 * the fixed world-tile grid; persistence and GUI never need to repeat those invariants.
 */
public final class ManualChunkGroupLayout {
    private static final Pattern ID = Pattern.compile("[a-zA-Z0-9_:-]{1,64}");
    private static final Set<String> ICONS = Set.of("", "*", "+", "#", "!", "@", "$", "^", "~", "X", "?");

    private ManualChunkGroupLayout() {}

    /** Creates the default manual partition: one independently editable group for every world tile. */
    public static List<Group> singletons(List<WorldTile> tiles) {
        return tiles.stream().map(tile -> new Group("manual_" + tile.id(), tile.name(), "", List.of(tile.id()))).toList();
    }

    /** Validates and applies one complete manual partition without touching storage or networking. */
    public static Applied apply(List<WorldTile> source, List<Group> groups) {
        Map<String, WorldTile> byId = new LinkedHashMap<>();
        Map<Long, String> tileAt = new HashMap<>();
        for (WorldTile tile : source) {
            if (byId.put(tile.id(), tile) != null) throw new IllegalArgumentException("duplicate world tile " + tile.id());
            tileAt.put(key(tile.mapX(), tile.mapZ()), tile.id());
        }
        if (groups.isEmpty()) throw new IllegalArgumentException("at least one group is required");

        Set<String> claimed = new HashSet<>();
        Set<String> groupIds = new HashSet<>();
        Map<String, Group> owner = new HashMap<>();
        List<Region> regions = new ArrayList<>(groups.size());
        for (Group group : groups) {
            validateGroup(group, byId, groupIds, claimed);
            requireConnected(group, byId, tileAt);
            group.tileIds().forEach(tileId -> owner.put(tileId, group));
            regions.add(new Region(group.id(), group.tileIds(), group.name(), storySeed(group.id()), group.icon(),
                    group.site(), group.resources(), group.configured()));
        }
        if (!claimed.equals(byId.keySet())) {
            Set<String> missing = new HashSet<>(byId.keySet());
            missing.removeAll(claimed);
            throw new IllegalArgumentException("world tiles are not assigned: " + missing.stream().sorted().limit(3).toList());
        }

        List<WorldTile> tiles = source.stream().map(tile -> {
            Group group = owner.get(tile.id());
            return new WorldTile(tile.id(), tile.mapX(), tile.mapZ(), group.id(), tile.name(), tile.color(),
                    tile.worldX(), tile.worldZ(), tile.discovered(), tile.biomeId(), tile.description(), tile.sites(), tile.resources());
        }).toList();
        return new Applied(tiles, regions);
    }

    private static void validateGroup(Group group, Map<String, WorldTile> byId, Set<String> groupIds, Set<String> claimed) {
        if (!ID.matcher(group.id()).matches()) throw new IllegalArgumentException("invalid group id " + group.id());
        if (!groupIds.add(group.id())) throw new IllegalArgumentException("duplicate group id " + group.id());
        if (group.name().isBlank() || group.name().length() > 48) throw new IllegalArgumentException("group name must contain 1-48 characters");
        if (!ICONS.contains(group.icon())) throw new IllegalArgumentException("unknown built-in icon " + group.icon());
        if (group.site().length() > 96) throw new IllegalArgumentException("group site must contain at most 96 characters");
        if (group.resources().length() > 160) throw new IllegalArgumentException("group resources must contain at most 160 characters");
        if (group.tileIds().isEmpty()) throw new IllegalArgumentException("group " + group.id() + " has no world tiles");
        for (String tileId : group.tileIds()) {
            if (!byId.containsKey(tileId)) throw new IllegalArgumentException("unknown world tile " + tileId);
            if (!claimed.add(tileId)) throw new IllegalArgumentException("world tile is assigned more than once: " + tileId);
        }
    }

    private static void requireConnected(Group group, Map<String, WorldTile> byId, Map<Long, String> tileAt) {
        Set<String> members = Set.copyOf(group.tileIds());
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(group.tileIds().getFirst());
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (!visited.add(id)) continue;
            WorldTile tile = byId.get(id);
            for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                String neighbour = tileAt.get(key(tile.mapX() + direction[0], tile.mapZ() + direction[1]));
                if (neighbour != null && members.contains(neighbour) && !visited.contains(neighbour)) queue.addLast(neighbour);
            }
        }
        if (visited.size() != members.size()) throw new IllegalArgumentException("group " + group.id() + " must be connected");
    }

    private static long storySeed(String id) { return 0x4D414E55414CL ^ id.hashCode() * 0x9E3779B9L; }
    private static long key(int x, int z) { return (long) x << 32 ^ z & 0xFFFFFFFFL; }

    public record Group(String id, String name, String icon, String site, String resources, boolean configured,
                        List<String> tileIds) {
        public Group { tileIds = List.copyOf(tileIds); }
        public Group(String id, String name, String icon, List<String> tileIds) {
            this(id, name, icon, "", "", false, tileIds);
        }
    }

    public record Applied(List<WorldTile> tiles, List<Region> regions) {
        public Applied { tiles = List.copyOf(tiles); regions = List.copyOf(regions); }
    }
}
