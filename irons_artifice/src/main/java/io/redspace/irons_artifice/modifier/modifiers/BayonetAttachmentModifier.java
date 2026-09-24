package io.redspace.irons_artifice.modifier.modifiers;

import io.redspace.irons_artifice.IronsArtifice;
import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.item.AttachmentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class BayonetAttachmentModifier implements GunModifier {

    @Override
    public void apply(ShotComponentMap components) {
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        builder.accept(Component.translatable("irons_artifice.modifier.bayonet").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public Optional<DataComponentPatch> getPatch() {
        // PORT-BLOCKED: KineticWeapon charge damage is not in 1.21.1; attachment render remains.
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        builder.set(DataComponentRegistry.ATTACHMENT.get(), new AttachmentMap(Map.of(
                GunBones.SOCKET_BAYONET, IronsArtifice.id("iron_bayonet")
        )));
        return Optional.of(builder.build());
    }
}
