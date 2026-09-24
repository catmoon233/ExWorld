package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Painter pathway passives. Color sense vision is drawn on the client from the synced skill list. */
public final class PainterPassives {
    public static final ResourceLocation COLOR_SENSE = ResourceLocation.fromNamespaceAndPath("lotm", "color_sense");
    public static final ResourceLocation RECORDER = ResourceLocation.fromNamespaceAndPath("lotm", "recorder");

    private static final Item[] RARE = {
            Items.EMERALD, Items.GOLD_INGOT, Items.LAPIS_LAZULI, Items.BOOK,
            Items.EXPERIENCE_BOTTLE, Items.SPYGLASS, Items.NAME_TAG, Items.AMETHYST_SHARD
    };

    private PainterPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                COLOR_SENSE,
                "passive.lotm.color_sense",
                "passive.lotm.color_sense.desc",
                "minecraft:spyglass",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                RECORDER,
                "passive.lotm.recorder",
                "passive.lotm.recorder.desc",
                "minecraft:writable_book",
                PassiveTrigger.KILL,
                0,
                PainterPassives::record));
    }

    private static boolean record(net.minecraft.server.level.ServerPlayer player, net.exmo.lotm.sequence.PassiveContext context) {
        if (!(context.other() instanceof LivingEntity dead) || !(player.level() instanceof ServerLevel level)) return false;
        var random = player.getRandom();
        boolean dropped = false;
        if (random.nextFloat() < 0.45F) {
            ExperienceOrb.award(level, dead.position(), 4 + random.nextInt(8));
            dropped = true;
        }
        if (random.nextFloat() < 0.18F) {
            Item item = RARE[random.nextInt(RARE.length)];
            int count = item == Items.LAPIS_LAZULI || item == Items.AMETHYST_SHARD ? 2 + random.nextInt(3) : 1;
            ItemEntity entity = new ItemEntity(level, dead.getX(), dead.getY() + 0.2, dead.getZ(), new ItemStack(item, count));
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
            dropped = true;
        }
        return dropped;
    }
}
