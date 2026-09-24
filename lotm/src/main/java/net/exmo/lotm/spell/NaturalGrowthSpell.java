package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Mature a 3x3 of crops and heal a little. 20 mana, 30s cooldown. */
public final class NaturalGrowthSpell extends InstantSpell {
    public static final float HEAL = 4.0F;

    public NaturalGrowthSpell() {
        super("natural_growth", SchoolRegistry.NATURE_RESOURCE, 30.0, 20, SoundEvents.BONE_MEAL_USE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BONE_MEAL_USE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos center = anchor(server, caster);
        int grown = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (mature(server, pos)) {
                        grown++;
                        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5,
                                4, 0.25, 0.2, 0.25, 0.0);
                    }
                }
            }
        }
        caster.heal(HEAL);
        if (grown == 0) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, caster.getX(), caster.getY() + 0.5, caster.getZ(), 6, 0.4, 0.2, 0.4, 0.0);
        }
    }

    private static BlockPos anchor(ServerLevel level, LivingEntity caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(5.0));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.BLOCK) return caster.blockPosition();
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.FARMLAND)) return pos.above();
        if (isCrop(state)) return pos;
        if (isCrop(level.getBlockState(pos.above()))) return pos.above();
        return pos;
    }

    static boolean mature(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop) {
            if (crop.getAge(state) >= crop.getMaxAge()) return false;
            level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), 2);
            return true;
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            if (state.getValue(NetherWartBlock.AGE) >= 3) return false;
            level.setBlock(pos, state.setValue(NetherWartBlock.AGE, 3), 2);
            return true;
        }
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            if (state.getValue(SweetBerryBushBlock.AGE) >= 3) return false;
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 3), 2);
            return true;
        }
        if (state.getBlock() instanceof CocoaBlock) {
            if (state.getValue(CocoaBlock.AGE) >= 2) return false;
            level.setBlock(pos, state.setValue(CocoaBlock.AGE, 2), 2);
            return true;
        }
        if (state.getBlock() instanceof StemBlock) {
            if (state.getValue(StemBlock.AGE) >= 7) return false;
            level.setBlock(pos, state.setValue(StemBlock.AGE, 7), 2);
            return true;
        }
        return false;
    }

    private static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || state.getBlock() instanceof CocoaBlock
                || state.getBlock() instanceof StemBlock;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.natural_growth.guide"));
    }
}
