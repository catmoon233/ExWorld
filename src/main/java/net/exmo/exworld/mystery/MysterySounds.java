package net.exmo.exworld.mystery;

import net.exmo.exworld.Exworld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Recorded announcements can be supplied later as assets/exworld/sounds/mystery/<id>.ogg. */
public final class MysterySounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Exworld.MODID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> CUES = new LinkedHashMap<>();

    static {
        for (String id : new String[] {"identity_reveal", "hunt_start", "clue_found", "linked_death", "rewind",
                "cycle_recap", "memory_wait", "board_train", "ending_chase", "ritual_complete",
                "ending_bad", "ending_normal", "ending_perfect"}) {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "mystery." + id);
            CUES.put(id, SOUNDS.register("mystery." + id, () -> SoundEvent.createVariableRangeEvent(name)));
        }
    }

    private MysterySounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    public static SoundEvent cue(String id) {
        DeferredHolder<SoundEvent, SoundEvent> value = CUES.get(id);
        return value == null ? null : value.get();
    }
    public static boolean known(String id) { return CUES.containsKey(id); }
}
