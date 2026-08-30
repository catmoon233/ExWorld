package net.exmo.exworld.battle.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controls how native Iron spell damage is folded back into the authoritative battle model.
 * Persistent and area spells cannot use the old "first entity hit completes the cast" rule.
 */
public final class IronHitWindow {
    public enum Policy { SINGLE, EACH_TARGET_ONCE, REPEATING }

    private final Policy policy;
    private final UUID lockedTarget;
    private final Map<UUID, Long> acceptedAtTick = new HashMap<>();

    public IronHitWindow(Policy policy, UUID lockedTarget) {
        this.policy = policy;
        this.lockedTarget = lockedTarget;
    }

    /** Channeled beams and persistent zones damage every tick they stay on a target. */
    private static final Set<String> REPEATING_SPELLS = Set.of(
            ":cone_of_cold", ":wall_of_fire",
            ":fire_breath", ":ray_of_frost", ":poison_breath", ":dragon_breath",
            ":electrocute", ":ray_of_siphoning", ":sunbeam");

    /** Single-burst area spells hit each target exactly once. */
    private static final Set<String> MULTI_HIT_SPELLS = Set.of(
            ":fireball", ":chain_lightning",
            ":shockwave", ":frostwave", ":stomp", ":divine_smite", ":ice_spikes");

    public static Policy policyFor(String skillId) {
        if (REPEATING_SPELLS.stream().anyMatch(skillId::endsWith)) return Policy.REPEATING;
        if (MULTI_HIT_SPELLS.stream().anyMatch(skillId::endsWith)) return Policy.EACH_TARGET_ONCE;
        return Policy.SINGLE;
    }

    public boolean accepts(UUID actualTarget, long serverTick) {
        if (policy == Policy.SINGLE && lockedTarget != null && !lockedTarget.equals(actualTarget)) return false;
        Long previousTick = acceptedAtTick.get(actualTarget);
        return switch (policy) {
            case SINGLE -> acceptedAtTick.isEmpty();
            case EACH_TARGET_ONCE -> previousTick == null;
            case REPEATING -> previousTick == null || previousTick < serverTick;
        };
    }

    public void record(UUID actualTarget, long serverTick) {
        acceptedAtTick.put(actualTarget, serverTick);
    }

    public boolean complete() {
        return policy == Policy.SINGLE && !acceptedAtTick.isEmpty();
    }

    public Policy policy() {
        return policy;
    }
}
