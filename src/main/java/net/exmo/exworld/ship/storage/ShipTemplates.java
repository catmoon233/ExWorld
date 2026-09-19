package net.exmo.exworld.ship.storage;

import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Built-in ship templates. Pure Java so tests can run without a Level. */
public final class ShipTemplates {
    public static final String TEMPLE_ID = "temple";

    private static final String STONE = "minecraft:stone_bricks";
    private static final String PLANKS = "minecraft:dark_oak_planks";
    private static final String PILLAR = "minecraft:quartz_pillar[axis=y]";
    private static final String ROOF = "minecraft:red_terracotta";
    private static final String GOLD = "minecraft:gold_block";
    private static final String GLOW = "minecraft:glowstone";
    private static final String CHEST = "minecraft:chest[facing=south]";
    private static final String BARREL = "minecraft:barrel[facing=up]";
    private static final String HELM = "exworld:ship_helm";
    private static final String CORE = "exworld:ship_core";

    private ShipTemplates() {}

    /** 神庙飞舟：石砖甲板 + 红陶殿顶 + 萤石引擎 + 舱室，四个部位不重叠。 */
    public static ShipTemplate temple() {
        Builder b = new Builder();
        // 龙骨 / 船底
        b.fill(b.hull, STONE, 3, 5, 0, 1, 1);
        b.fill(b.hull, STONE, 2, 6, 0, 2, 8);
        b.fill(b.hull, STONE, 3, 5, 0, 9, 9);
        // 甲板（尾部 z=10 留给引擎）
        b.fill(b.hull, STONE, 0, 8, 1, 0, 9);
        b.fill(b.hull, PLANKS, 1, 7, 1, 1, 9);
        // 核心 / 舵机
        b.put(b.hull, CORE, 4, 1, 5);
        b.put(b.hull, HELM, 4, 2, 1);
        // 立柱
        for (int x : new int[]{1, 7}) {
            for (int z : new int[]{1, 5, 9}) {
                b.put(b.hull, PILLAR, x, 2, z);
            }
        }
        // 殿顶（前部 z=1 留空给舵机）
        b.fill(b.roof, ROOF, 1, 7, 3, 2, 9);
        b.fill(b.roof, ROOF, 2, 6, 4, 3, 8);
        b.fill(b.roof, ROOF, 3, 5, 5, 4, 7);
        b.fill(b.roof, ROOF, 4, 4, 6, 5, 6);
        b.put(b.roof, GOLD, 4, 6, 6);
        // 引擎
        b.fill(b.engine, GLOW, 3, 5, 1, 10, 10);
        b.put(b.engine, GLOW, 4, 2, 10);
        // 舱室
        b.put(b.cabin, CHEST, 4, 2, 8);
        b.put(b.cabin, BARREL, 3, 2, 8);

        ShipPart hull = new ShipPart("hull", "船体", 0xFF55AAFF, ShipOccupancy.sorted(b.hull), List.of(), 0.4);
        ShipPart roof = new ShipPart("roof", "殿顶", 0xFFFF8866, ShipOccupancy.sorted(b.roof), List.of(), 0.3);
        ShipPart engine = new ShipPart("engine", "引擎", 0xFFEEDD55, ShipOccupancy.sorted(b.engine), List.of(), 0.9);
        ShipPart cabin = new ShipPart("cabin", "舱室", 0xFF88DD77, ShipOccupancy.sorted(b.cabin), List.of(), 0.3);
        return new ShipTemplate(TEMPLE_ID, "神庙飞舟", ShipHull.of(b.blocks), List.of(hull, roof, engine, cabin), PartSelection.empty());
    }

    private static final class Builder {
        final List<ShipBlock> blocks = new ArrayList<>();
        final Set<Integer> hull = new TreeSet<>();
        final Set<Integer> roof = new TreeSet<>();
        final Set<Integer> engine = new TreeSet<>();
        final Set<Integer> cabin = new TreeSet<>();

        void put(Set<Integer> part, String block, int x, int y, int z) {
            blocks.add(new ShipBlock(x, y, z, block, List.of()));
            part.add(ShipOccupancy.pack(x, y, z));
        }

        void fill(Set<Integer> part, String block, int x0, int x1, int y, int z0, int z1) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    put(part, block, x, y, z);
                }
            }
        }
    }
}
