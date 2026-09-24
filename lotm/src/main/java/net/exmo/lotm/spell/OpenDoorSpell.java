package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.ApprenticePassives;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Step through a looked-at wall. Bedrock, barrier, and similar blocks refuse the door. */
public final class OpenDoorSpell extends InstantSpell {
    public static final double BASE_RANGE = 8.0;

    public OpenDoorSpell() {
        super("open_door", 12.0, 15, SchoolRegistry.ENDER_RESOURCE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        double range = entity.level().isClientSide() ? BASE_RANGE + 2.0 : ApprenticePassives.doorRange(entity);
        if (exit(entity, range) != null) return true;
        LotmSupport.tell(entity, "spell.lotm.open_door.blocked");
        return false;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        Vec3 dest = exit(caster, ApprenticePassives.doorRange(caster));
        if (dest == null) return;
        Vec3 from = caster.position();
        if (caster instanceof ServerPlayer player) {
            player.teleportTo(level, dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
        } else {
            caster.teleportTo(dest.x, dest.y, dest.z);
        }
        caster.resetFallDistance();
        level.playSound(null, BlockPos.containing(dest), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 16, 0.3, 0.5, 0.3, 0.1);
        level.sendParticles(ParticleTypes.PORTAL, dest.x, dest.y + 1.0, dest.z, 16, 0.3, 0.5, 0.3, 0.1);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData data, boolean cancelled) {
        super.onServerCastComplete(level, spellLevel, entity, data, cancelled);
        if (cancelled || !(entity instanceof ServerPlayer player) || !ApprenticePassives.hasSpatial(player)) return;
        int reduced = Math.max(1, (int) Math.round(getSpellCooldown() * 0.8));
        data.getPlayerCooldowns().addCooldown(getSpellId(), reduced);
        data.getPlayerCooldowns().syncToPlayer(player);
    }

    static Vec3 exit(LivingEntity caster, double range) {
        if (caster == null || range <= 0) return null;
        Level level = caster.level();
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        boolean entered = false;
        BlockPos obstacle = null;
        for (double distance = 0.4; distance <= range; distance += 0.25) {
            Vec3 point = eye.add(look.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            if (!level.hasChunkAt(pos)) return null;
            BlockState state = level.getBlockState(pos);
            if (forbidden(state)) return null;
            if (blocking(level, pos, state)) {
                entered = true;
                obstacle = pos;
                continue;
            }
            if (!entered || obstacle == null) continue;
            Vec3 feet = new Vec3(point.x, caster.getY(), point.z);
            BlockPos column = BlockPos.containing(feet);
            if (column.getX() == obstacle.getX() && column.getZ() == obstacle.getZ()) continue;
            if (fits(caster, feet)) return feet;
        }
        return null;
    }

    private static boolean fits(LivingEntity caster, Vec3 feet) {
        Vec3 delta = feet.subtract(caster.position());
        if (delta.lengthSqr() < 0.16) return false;
        if (!caster.isFree(delta.x, delta.y, delta.z)) return false;
        BlockPos standing = BlockPos.containing(feet);
        Level level = caster.level();
        return !forbidden(level.getBlockState(standing))
                && !forbidden(level.getBlockState(standing.above()))
                && level.getFluidState(standing).isEmpty();
    }

    private static boolean blocking(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock || state.getBlock() instanceof TrapDoorBlock) {
            return true;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean forbidden(BlockState state) {
        var block = state.getBlock();
        return block == Blocks.BEDROCK
                || block == Blocks.BARRIER
                || block == Blocks.COMMAND_BLOCK
                || block == Blocks.CHAIN_COMMAND_BLOCK
                || block == Blocks.REPEATING_COMMAND_BLOCK
                || block == Blocks.STRUCTURE_VOID
                || block == Blocks.JIGSAW
                || block == Blocks.REINFORCED_DEEPSLATE
                || block == Blocks.END_PORTAL
                || block == Blocks.END_GATEWAY
                || block == Blocks.MOVING_PISTON;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.open_door.guide"));
    }
}
