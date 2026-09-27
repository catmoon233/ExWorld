package net.exmo.exphone;

import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Optional Exposure camera. Gives a camera and film, then starts its viewfinder. */
final class ExposurePhotos {
    private ExposurePhotos() {}

    static boolean installed() {
        return cameraItem() != Items.AIR || loaded();
    }

    private static boolean loaded() {
        try {
            return net.neoforged.fml.ModList.get().isLoaded("exposure");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Item cameraItem() {
        Item known = item("exposure:camera");
        if (known != Items.AIR) return known;
        for (var id : BuiltInRegistries.ITEM.keySet()) {
            if (!"exposure".equals(id.getNamespace())) continue;
            String path = id.getPath();
            if (!path.contains("camera") || path.contains("stand") || path.contains("part")) continue;
            Item found = BuiltInRegistries.ITEM.get(id);
            if (found != Items.AIR) return found;
        }
        return Items.AIR;
    }

    static String open(ServerPlayer player) {
        Item camera = cameraItem();
        Item color = item("exposure:color_film");
        Item film = color != Items.AIR ? color : item("exposure:black_and_white_film");
        if (camera == Items.AIR) return loaded() ? "找不到曝光相机" : "未安装 Exposure";
        if (!player.getInventory().contains(stack -> stack.is(camera))) player.getInventory().add(new ItemStack(camera));
        if (film != Items.AIR && !player.getInventory().contains(stack -> stack.is(film))) player.getInventory().add(new ItemStack(film));
        select(player, camera);
        ItemStack held = player.getMainHandItem();
        if (!held.is(camera)) return "无法拿起曝光相机";
        try {
            Object item = held.getItem();
            Object active = item.getClass().getMethod("isActive", ItemStack.class).invoke(item, held);
            if (Boolean.TRUE.equals(active)) return "已打开曝光相机";
            item.getClass().getMethod("activateInHand", net.minecraft.world.entity.player.Player.class, ItemStack.class, InteractionHand.class)
                    .invoke(item, player, held, InteractionHand.MAIN_HAND);
        } catch (ReflectiveOperationException ignored) {
            held.use(player.level(), player, InteractionHand.MAIN_HAND);
        }
        return "已打开曝光相机";
    }

    private static void select(ServerPlayer player, Item item) {
        Inventory inventory = player.getInventory();
        int slot = inventory.findSlotMatchingItem(new ItemStack(item));
        if (slot < 0) return;
        if (slot > 8) {
            int hotbar = inventory.selected;
            ItemStack held = inventory.getItem(hotbar);
            inventory.setItem(hotbar, inventory.getItem(slot));
            inventory.setItem(slot, held);
            slot = hotbar;
        }
        inventory.selected = slot;
        player.connection.send(new ClientboundSetCarriedItemPacket(slot));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    static ItemStack photograph(ServerPlayer player, int index) {
        int seen = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isPhoto(stack)) continue;
            if (seen++ == index) return stack;
        }
        return ItemStack.EMPTY;
    }

    static com.google.gson.JsonArray album(ServerPlayer player) {
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        int index = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!isPhoto(stack)) continue;
            com.google.gson.JsonObject item = new com.google.gson.JsonObject();
            item.addProperty("slot", index++);
            item.addProperty("name", stack.getHoverName().getString());
            array.add(item);
        }
        return array;
    }

    private static boolean isPhoto(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.startsWith("exposure:") && id.contains("photograph");
    }
}
