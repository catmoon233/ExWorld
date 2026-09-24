package net.exmo.exworld.npc.data;

import java.util.ArrayList;
import java.util.List;

/** Wearables, stock and skin source. The texture field still holds the path, player name or URL. */
public record NpcLoadout(boolean slim, String skinKind, int restockMinutes, List<Gear> equipment, List<Stock> inventory) {
    public static final NpcLoadout EMPTY = new NpcLoadout(false, "resource", 20, List.of(), List.of());

    public NpcLoadout {
        String kind = skinKind == null ? "" : skinKind.trim().toLowerCase();
        skinKind = kind.equals("player") || kind.equals("url") ? kind : "resource";
        restockMinutes = restockMinutes <= 0 ? 20 : Math.min(restockMinutes, 240);
        equipment = equipment == null ? List.of() : List.copyOf(equipment);
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
    }

    public record Gear(String slot, String item) {
        public Gear {
            slot = slot == null ? "" : slot.trim().toLowerCase();
            item = item == null ? "" : item.trim();
        }
    }

    public record Stock(String item, int count) {
        public Stock {
            item = item == null ? "" : item.trim();
            count = Math.max(0, count);
        }
    }

    public String format() {
        StringBuilder gear = new StringBuilder();
        for (Gear piece : equipment) {
            if (!gear.isEmpty()) gear.append(',');
            gear.append(piece.slot()).append('=').append(piece.item());
        }
        StringBuilder stock = new StringBuilder();
        for (Stock line : inventory) {
            if (!stock.isEmpty()) stock.append(',');
            stock.append(line.item()).append('*').append(line.count());
        }
        return "slim=" + slim + ";kind=" + skinKind + ";restock=" + restockMinutes + ";equip=" + gear + ";stock=" + stock;
    }

    public static NpcLoadout parse(String raw) {
        if (raw == null || raw.isBlank()) return EMPTY;
        boolean slim = false;
        String kind = "resource";
        int restock = 20;
        List<Gear> gear = new ArrayList<>();
        List<Stock> stock = new ArrayList<>();
        for (String part : raw.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String key = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            switch (key) {
                case "slim" -> slim = value.equalsIgnoreCase("true") || value.equals("1");
                case "kind" -> kind = value;
                case "restock" -> {
                    try { restock = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                }
                case "equip" -> {
                    if (value.isBlank()) break;
                    for (String piece : value.split(",")) {
                        int cut = piece.indexOf('=');
                        if (cut > 0) gear.add(new Gear(piece.substring(0, cut), piece.substring(cut + 1)));
                    }
                }
                case "stock" -> {
                    if (value.isBlank()) break;
                    for (String line : value.split(",")) {
                        int star = line.lastIndexOf('*');
                        if (star <= 0) continue;
                        try { stock.add(new Stock(line.substring(0, star), Integer.parseInt(line.substring(star + 1).trim()))); }
                        catch (NumberFormatException ignored) {}
                    }
                }
                default -> {}
            }
        }
        return new NpcLoadout(slim, kind, restock, gear, stock);
    }
}
