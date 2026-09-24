package net.exmo.lotm.client;

import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;

/** Local particles for Resource Sense. Only the caster's client draws them. */
public final class ResourceSenseClient {
    private static final int HORIZONTAL = 12;
    private static final int VERTICAL = 8;

    private ResourceSenseClient() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        if (!minecraft.player.hasEffect(LotmEffects.RESOURCE_SENSE)) return;
        if ((minecraft.level.getGameTime() & 7L) != 0L) return;
        BlockPos center = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int ores = 0;
        int plants = 0;
        var level = minecraft.level;
        for (int dx = -HORIZONTAL; dx <= HORIZONTAL && (ores < 48 || plants < 48); dx++) {
            for (int dy = -VERTICAL; dy <= VERTICAL && (ores < 48 || plants < 48); dy++) {
                for (int dz = -HORIZONTAL; dz <= HORIZONTAL; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (ores < 48 && state.is(Tags.Blocks.ORES)) {
                        spark(minecraft, ParticleTypes.END_ROD, cursor.getX() + 0.5, cursor.getY() + 0.5, cursor.getZ() + 0.5);
                        ores++;
                    } else if (plants < 48 && plant(state)) {
                        spark(minecraft, ParticleTypes.HAPPY_VILLAGER, cursor.getX() + 0.5, cursor.getY() + 0.4, cursor.getZ() + 0.5);
                        plants++;
                    }
                }
            }
        }
        int mobs = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, minecraft.player.getBoundingBox().inflate(16.0),
                living -> living != minecraft.player && living.isAlive() && !(living instanceof ArmorStand))) {
            if (minecraft.player.distanceToSqr(living) > 16.0 * 16.0) continue;
            spark(minecraft, ParticleTypes.SOUL, living.getX(), living.getY() + living.getBbHeight() * 0.6, living.getZ());
            if (++mobs >= 16) break;
        }
    }

    private static void spark(Minecraft minecraft, ParticleOptions particle, double x, double y, double z) {
        minecraft.level.addParticle(particle, x, y, z, 0.0, 0.02, 0.0);
    }

    private static boolean plant(BlockState state) {
        if (state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)) return true;
        var block = state.getBlock();
        return block == Blocks.SUGAR_CANE
                || block == Blocks.CACTUS
                || block == Blocks.BAMBOO
                || block == Blocks.BAMBOO_SAPLING
                || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.CAVE_VINES
                || block == Blocks.CAVE_VINES_PLANT
                || block == Blocks.NETHER_WART
                || block == Blocks.BROWN_MUSHROOM
                || block == Blocks.RED_MUSHROOM
                || block == Blocks.KELP
                || block == Blocks.KELP_PLANT
                || block == Blocks.COCOA
                || block == Blocks.PITCHER_CROP
                || block == Blocks.TORCHFLOWER_CROP
                || block == Blocks.TWISTING_VINES
                || block == Blocks.TWISTING_VINES_PLANT
                || block == Blocks.WEEPING_VINES
                || block == Blocks.WEEPING_VINES_PLANT;
    }
}
