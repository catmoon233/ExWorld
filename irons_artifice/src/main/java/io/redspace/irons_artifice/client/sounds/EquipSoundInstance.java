package io.redspace.irons_artifice.client.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class EquipSoundInstance extends AbstractTickableSoundInstance {
    private final Item gunItem;
    private final Player player;
    private final float baseVolume;

    public EquipSoundInstance(
            SoundEvent sound,
            SoundSource source,
            float volume,
            float pitch,
            RandomSource random,
            Item gunItem,
            int delay
    ) {
        super(sound, source, random);
        this.baseVolume = volume;
        this.volume = 0;
        this.pitch = pitch;
        this.attenuation = Attenuation.NONE;
        this.relative = true;
        this.gunItem = gunItem;
        this.player = Minecraft.getInstance().player;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (player.getMainHandItem().getItem() != gunItem && player.getOffhandItem().getItem() != gunItem) {
            stop();
        } else {
            this.volume = baseVolume;
        }
    }
}
