package io.redspace.irons_artifice.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

public final class ExtendedRaiderTypes {
    public static final EnumProxy<Raid.RaiderType> ILLIFICER = new EnumProxy<>(
            Raid.RaiderType.class,
            (Supplier<EntityType<? extends Raider>>) EntityRegistry.ILLIFICER::get,
            new int[]{0, 0, 0, 1, 0, 1, 2, 3}
    );
}
