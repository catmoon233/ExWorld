package net.exmo.lotm.phone;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.exmo.exworld.progress.PlayerProgressSystem;
import net.exmo.exworld.progress.PlayerResourceVault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/** WiFi coverage and SIM credit / mobile data. WeChat needs one of the two links. */
final class PhoneWifi {
    static final int MIN_RANGE = 4;
    static final int MAX_RANGE = 64;
    private static final int DATA_PER_CREDIT = 10;

    private PhoneWifi() {}

    static String gate(ServerPlayer player) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (linked(player, self)) return "";
        PhoneData.Sim sim = data.balance(self.number);
        if (sim == null || sim.data <= 0) return "未连接 WiFi，且没有可用移动数据流量";
        sim.data--;
        data.touch();
        return "";
    }

    static boolean online(ServerPlayer player) {
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (linked(player, self)) return true;
        PhoneData.Sim sim = data.balance(self.number);
        return sim != null && sim.data > 0;
    }

    static String current(ServerPlayer player) {
        PhoneData.Profile self = PhoneData.get(player).ensure(player);
        WifiRouterBlockEntity router = live(player, self);
        return router == null ? "" : router.name();
    }

    static int credit(ServerPlayer player) {
        PhoneData.Sim sim = PhoneData.get(player).balance(PhoneData.get(player).ensure(player).number);
        return sim == null ? 0 : sim.credit;
    }

    static int data(ServerPlayer player) {
        PhoneData.Sim sim = PhoneData.get(player).balance(PhoneData.get(player).ensure(player).number);
        return sim == null ? 0 : sim.data;
    }

    static String connect(ServerPlayer player, String name, String password) {
        String wanted = name == null ? "" : name.trim();
        if (wanted.isBlank()) return "请填写 WiFi 名称";
        String given = password == null ? "" : password.trim();
        boolean outside = false;
        boolean wrong = false;
        for (WifiRouterBlockEntity router : scan(player)) {
            if (!router.name().equalsIgnoreCase(wanted)) continue;
            if (!router.covers(player)) {
                outside = true;
                continue;
            }
            if (!router.password().equals(given)) {
                wrong = true;
                continue;
            }
            PhoneData data = PhoneData.get(player);
            PhoneData.Profile self = data.ensure(player);
            self.wifiDim = player.serverLevel().dimension().location().toString();
            self.wifiX = router.getBlockPos().getX();
            self.wifiY = router.getBlockPos().getY();
            self.wifiZ = router.getBlockPos().getZ();
            self.wifiPass = router.password();
            data.touch();
            return "已连接 " + router.name();
        }
        if (wrong) return "密码错误";
        return outside ? "不在覆盖范围内" : "范围内没有这个 WiFi";
    }

    static String disconnect(ServerPlayer player) {
        PhoneData data = PhoneData.get(player);
        clear(data.ensure(player));
        data.touch();
        return "已断开 WiFi";
    }

    static String save(ServerPlayer player, int x, int y, int z, String name, String password, int range) {
        BlockPos pos = new BlockPos(x, y, z);
        if (player.distanceToSqr(pos.getCenter()) > 64) return "离路由器太远";
        BlockEntity entity = player.serverLevel().getBlockEntity(pos);
        if (!(entity instanceof WifiRouterBlockEntity router)) return "这里没有路由器";
        if (!router.canEdit(player)) return "只有放置者可以设置";
        router.configure(name, password, range <= 0 ? 16 : range);
        return "已保存 " + router.name() + " · 范围 " + router.range() + " 格";
    }

    static String topup(ServerPlayer player, int amount) {
        if (amount <= 0 || amount > 100_000) return "金额无效";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (self.number.isBlank()) return "请先安装 SIM 卡";
        if (!PlayerProgressSystem.vault().take(player.server, player.getUUID(), PlayerResourceVault.GOLD, amount)) return "金币不足";
        PhoneData.Sim sim = data.sim(self.number);
        sim.credit = Math.min(1_000_000, sim.credit + amount);
        data.touch();
        return "已为 " + self.number + " 充值 " + amount + " 话费";
    }

    static String buyData(ServerPlayer player, int amount) {
        if (amount <= 0 || amount > 100_000) return "金额无效";
        PhoneData data = PhoneData.get(player);
        PhoneData.Profile self = data.ensure(player);
        if (self.number.isBlank()) return "请先安装 SIM 卡";
        PhoneData.Sim sim = data.sim(self.number);
        if (sim.credit < amount) return "话费不足";
        sim.credit -= amount;
        sim.data = Math.min(1_000_000, sim.data + amount * DATA_PER_CREDIT);
        data.touch();
        return "已用 " + amount + " 话费兑换 " + (amount * DATA_PER_CREDIT) + " 流量";
    }

    static JsonArray nearby(ServerPlayer player) {
        JsonArray array = new JsonArray();
        for (WifiRouterBlockEntity router : scan(player)) {
            if (!router.covers(player)) continue;
            JsonObject item = new JsonObject();
            item.addProperty("name", router.name());
            item.addProperty("range", router.range());
            item.addProperty("dist", (int) Math.sqrt(player.distanceToSqr(router.getBlockPos().getCenter())));
            array.add(item);
        }
        return array;
    }

    private static List<WifiRouterBlockEntity> scan(ServerPlayer player) {
        List<WifiRouterBlockEntity> found = new ArrayList<>();
        ServerLevel level = player.serverLevel();
        ChunkPos center = new ChunkPos(player.blockPosition());
        int radius = (MAX_RANGE >> 4) + 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!level.hasChunk(center.x + dx, center.z + dz)) continue;
                LevelChunk chunk = level.getChunk(center.x + dx, center.z + dz);
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity instanceof WifiRouterBlockEntity router) found.add(router);
                }
            }
        }
        return found;
    }

    private static boolean linked(ServerPlayer player, PhoneData.Profile self) {
        WifiRouterBlockEntity router = live(player, self);
        if (router == null || !router.password().equals(self.wifiPass) || !router.covers(player)) {
            if (!self.wifiDim.isBlank()) {
                clear(self);
                PhoneData.get(player).touch();
            }
            return false;
        }
        return true;
    }

    private static WifiRouterBlockEntity live(ServerPlayer player, PhoneData.Profile self) {
        if (self.wifiDim.isBlank() || !self.wifiDim.equals(player.serverLevel().dimension().location().toString())) return null;
        BlockEntity entity = player.serverLevel().getBlockEntity(new BlockPos(self.wifiX, self.wifiY, self.wifiZ));
        return entity instanceof WifiRouterBlockEntity router ? router : null;
    }

    private static void clear(PhoneData.Profile self) {
        self.wifiDim = "";
        self.wifiPass = "";
    }
}
