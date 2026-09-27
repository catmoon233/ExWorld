package net.exmo.exphone;

import com.google.gson.JsonObject;
import net.exmo.exphone.api.PhoneEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server side of the ATM paper form. */
public final class AtmActions {
    private AtmActions() {}

    public static String openJson(ServerPlayer player, String status) {
        JsonObject root = new JsonObject();
        root.addProperty("openAtm", true);
        root.addProperty("gold", PhoneEconomy.balance(player.server, player.getUUID(), PhoneEconomy.GOLD));
        root.addProperty("admin", player.hasPermissions(2));
        root.addProperty("status", status == null ? "" : status);
        return root.toString();
    }

    public static String handle(ServerPlayer player, String action, int denom, int count, int amount) {
        if (!near(player)) return "附近没有 ATM";
        return switch (action) {
            case "atm_withdraw" -> withdraw(player, denom, count);
            case "atm_deposit" -> deposit(player, denom, count);
            case "atm_deposit_all" -> depositAll(player);
            case "atm_credit" -> credit(player, amount);
            default -> "未知操作";
        };
    }

    private static String withdraw(ServerPlayer player, int denom, int count) {
        long cost = Banknotes.cost(denom, count);
        if (cost < 0) return "面额或数量无效";
        Item bill = PhoneItems.bill(denom);
        if (bill == Items.AIR) return "没有这种纸币";
        if (!fits(player, bill, count)) return "背包空间不足";
        if (!PhoneEconomy.take(player.server, player.getUUID(), PhoneEconomy.GOLD, cost)) return "金币不足";
        give(player, bill, count);
        return "已提出 " + count + " 张 " + denom + " 金币纸币";
    }

    private static String deposit(ServerPlayer player, int denom, int count) {
        if (!Banknotes.valid(denom) || count <= 0) return "面额或数量无效";
        Item bill = PhoneItems.bill(denom);
        int removed = remove(player, bill, count);
        if (removed <= 0) return "没有这种纸币";
        long value = (long) denom * removed;
        PhoneEconomy.add(player.server, player.getUUID(), PhoneEconomy.GOLD, value);
        return "已存入 " + removed + " 张 " + denom + " 金币纸币";
    }

    private static String depositAll(ServerPlayer player) {
        long total = 0;
        int notes = 0;
        for (int denom : Banknotes.VALUES) {
            Item bill = PhoneItems.bill(denom);
            int removed = remove(player, bill, Integer.MAX_VALUE);
            if (removed <= 0) continue;
            total += (long) denom * removed;
            notes += removed;
        }
        if (notes <= 0) return "背包里没有纸币";
        PhoneEconomy.add(player.server, player.getUUID(), PhoneEconomy.GOLD, total);
        return "已存入 " + notes + " 张纸币，+" + total + " 金币";
    }

    private static String credit(ServerPlayer player, int amount) {
        if (!player.hasPermissions(2)) return "需要管理员";
        if (amount <= 0 || amount > 1_000_000_000) return "金额无效";
        long balance = PhoneEconomy.add(player.server, player.getUUID(), PhoneEconomy.GOLD, amount);
        return "已充值 " + amount + " 金币，余额 " + balance;
    }

    static boolean near(ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (player.serverLevel().getBlockState(origin.offset(x, y, z)).is(PhoneBlocks.ATM.get())) return true;
                }
            }
        }
        return false;
    }

    private static boolean fits(ServerPlayer player, Item item, int count) {
        int remaining = count;
        int max = Math.max(1, item.getDefaultMaxStackSize());
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) remaining -= max;
            else if (stack.is(item)) remaining -= Math.max(0, max - stack.getCount());
            if (remaining <= 0) return true;
        }
        return false;
    }

    private static void give(ServerPlayer player, Item item, int count) {
        int left = count;
        int max = Math.max(1, item.getDefaultMaxStackSize());
        while (left > 0) {
            int size = Math.min(left, max);
            ItemStack stack = new ItemStack(item, size);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            left -= size;
        }
    }

    private static int remove(ServerPlayer player, Item item, int count) {
        int left = count;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
        return count - left;
    }
}
