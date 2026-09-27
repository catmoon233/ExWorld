package net.exmo.exphone;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** UI blips. Minecraft's sound engine only loads ogg, so the files live under assets/exphone/sounds/phone. */
public final class PhoneSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ExPhone.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> CLICK = register("phone_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> PAGE = register("phone_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNLOCK = register("phone_unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEND = register("phone_send");

    private PhoneSounds() {}

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, path)));
    }
}
