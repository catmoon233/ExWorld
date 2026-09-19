package net.exmo.exworld.world.generation;

import com.mojang.serialization.MapCodec;
import net.exmo.exworld.Exworld;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Seed-aware inverted-pyramid island mask. RandomState rewires a fresh instance after {@link IslandLayout#bindSeed(long)}. */
public final class IslandField implements DensityFunction.SimpleFunction {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Exworld.MODID);
    public static final KeyDispatchDataCodec<IslandField> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(new IslandField(0L)));
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<IslandField>> TYPE =
            TYPES.register("island_field", () -> CODEC.codec());

    private final long seed;
    private final IslandLayout.Settings settings;

    public IslandField() { this(IslandLayout.worldSeed(), IslandLayout.boundSettings()); }
    public IslandField(long seed) { this(seed, IslandLayout.Settings.defaults()); }
    public IslandField(long seed, IslandLayout.Settings settings) {
        this.seed = seed;
        this.settings = settings;
    }

    public static void register(IEventBus bus) { TYPES.register(bus); }

    public long seed() { return seed; }
    public IslandLayout.Settings settings() { return settings; }

    @Override
    public double compute(FunctionContext context) {
        return IslandLayout.density(seed, settings, context.blockX(), context.blockY(), context.blockZ());
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new IslandField(IslandLayout.worldSeed(), IslandLayout.boundSettings()));
    }

    @Override
    public double minValue() { return -1.0; }

    @Override
    public double maxValue() { return 1.0; }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
