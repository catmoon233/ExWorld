package net.exmo.lotm.phone;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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
}
