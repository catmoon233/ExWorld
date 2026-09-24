package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

public record PassiveDefinition(
        ResourceLocation id,
        String nameKey,
        String descriptionKey,
        String iconItem,
        PassiveTrigger trigger,
        int cooldownTicks,
        PassiveEffect effect
) {
    public PassiveDefinition {
        if (id == null) throw new IllegalArgumentException("id");
        if (nameKey == null) nameKey = "";
        if (descriptionKey == null) descriptionKey = "";
        if (iconItem == null) iconItem = "minecraft:iron_sword";
        if (trigger == null) trigger = PassiveTrigger.TICK;
        if (cooldownTicks < 0) cooldownTicks = 0;
        if (effect == null) effect = (player, context) -> false;
    }

    @FunctionalInterface
    public interface PassiveEffect {
        /** @return true when the cooldown should start */
        boolean apply(net.minecraft.server.level.ServerPlayer player, PassiveContext context);
    }
}
