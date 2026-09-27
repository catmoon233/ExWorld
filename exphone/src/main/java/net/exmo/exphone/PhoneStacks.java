package net.exmo.exphone;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class PhoneStacks {
    private PhoneStacks() {}

    static String number(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("number");
    }

    static void setNumber(ItemStack stack, String number) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (number == null || number.isBlank()) tag.remove("number");
        else tag.putString("number", number);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    static java.util.Optional<PhoneModel> model(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return java.util.Optional.empty();
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return PhoneModel.Brand.parse(tag.getString("phoneBrand"))
                .flatMap(brand -> PhoneModel.Color.parse(tag.getString("phoneColor"))
                        .map(color -> new PhoneModel(brand, color)));
    }

    static void setModel(ItemStack stack, PhoneModel model) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString("phoneBrand", model.brand().name());
        tag.putString("phoneColor", model.color().name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
     }
 
     static int battery(ItemStack stack) {
         if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PhoneItem)) return 0;
         CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
         return tag.contains("battery") ? Math.clamp(tag.getInt("battery"), 0, 100) : 100;
     }
 
     static void addBattery(ItemStack stack, int delta) {
         if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PhoneItem)) return;
         CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
         tag.putInt("battery", Math.clamp(battery(stack) + delta, 0, 100));
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
     }
 
     static boolean useBattery(ItemStack stack) {
         if (battery(stack) <= 0) return false;
         addBattery(stack, -1);
         return true;
     }

    static ItemStack heldPhone(Player player) {
        if (player.getMainHandItem().getItem() instanceof PhoneItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof PhoneItem) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    static void clearMatching(Player player, String number) {
        if (number == null || number.isBlank()) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PhoneItem && number.equals(number(stack))) setNumber(stack, "");
        }
    }

    static void ensureModel(ItemStack stack, RandomSource random) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PhoneItem) || model(stack).isPresent()) return;
        PhoneModel.Brand[] brands = PhoneModel.Brand.values();
        PhoneModel.Color[] colors = PhoneModel.Color.values();
        setModel(stack, new PhoneModel(brands[random.nextInt(brands.length)], colors[random.nextInt(colors.length)]));
    }

    static boolean carrying(Player player) {
        return !phoneOf(player).isEmpty();
    }

    /** 手持的手机优先，其次搜整个物品栏；通话状态机用它判断“手机在不在身上”。 */
    static ItemStack phoneOf(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack held = heldPhone(player);
        if (!held.isEmpty()) return held;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof PhoneItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** 通话每分钟从玩家身上的手机扣 1% 电量；没有手机时不做任何事。 */
    static void drain(Player player) {
        ItemStack stack = phoneOf(player);
        if (!stack.isEmpty()) addBattery(stack, -1);
    }

    static void alert(ServerPlayer player, String text) {
        if (player == null || text == null || text.isBlank() || !carrying(player)) return;
        player.displayClientMessage(Component.translatable("message.exphone.phone.ping", text), false);
    }
}
