package net.exmo.lotm.sequence;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public record PassiveContext(
        PassiveTrigger trigger,
        ServerPlayer player,
        Entity other,
        float amount,
        LivingIncomingDamageEvent damageEvent
) {}
