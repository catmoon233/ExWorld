package net.exmo.lotm.client;

import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.client.player.Input;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/** Randomly turns the local player's movement while mind scramble is active. */
public final class MindScrambleClient {
    private MindScrambleClient() {}

    public static void input(MovementInputUpdateEvent event) {
        if (!event.getEntity().hasEffect(LotmEffects.MIND_SCRAMBLE)) return;
        Input input = event.getInput();
        if (input.forwardImpulse == 0.0F && input.leftImpulse == 0.0F) return;
        float forward = input.forwardImpulse;
        float left = input.leftImpulse;
        switch ((event.getEntity().tickCount / 8) & 3) {
            case 0 -> input.forwardImpulse = -forward;
            case 1 -> input.leftImpulse = -left;
            case 2 -> {
                input.forwardImpulse = left;
                input.leftImpulse = forward;
            }
            default -> {
                input.forwardImpulse = -left;
                input.leftImpulse = -forward;
            }
        }
    }
}
