package io.redspace.irons_artifice.registry;

import io.redspace.irons_artifice.IronsArtifice;
import io.redspace.irons_artifice.item.AttachmentMap;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.item.ReloadState;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, IronsArtifice.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagazineContents>> MAGAZINE =
            COMPONENTS.register("magazine", () -> DataComponentType.<MagazineContents>builder()
                    .persistent(MagazineContents.CODEC)
                    .networkSynchronized(MagazineContents.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReloadState>> RELOAD_STATE =
            COMPONENTS.register("reload_state", () -> DataComponentType.<ReloadState>builder()
                    .persistent(ReloadState.CODEC)
                    .networkSynchronized(ReloadState.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DataComponentPatch>> MODIFIER_PATCH =
            COMPONENTS.register("modifier_patch", () -> DataComponentType.<DataComponentPatch>builder()
                    .persistent(DataComponentPatch.CODEC)
                    .networkSynchronized(DataComponentPatch.STREAM_CODEC)
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> GUN_SPYGLASS =
            COMPONENTS.register("gun_spyglass", () -> DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
                    .build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AttachmentMap>> ATTACHMENT =
            COMPONENTS.register("attachment", () -> DataComponentType.<AttachmentMap>builder()
                    .persistent(AttachmentMap.CODEC)
                    .networkSynchronized(AttachmentMap.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
