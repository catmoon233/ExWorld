package io.redspace.irons_artifice.client.gun;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The entity whose held gun is currently being rendered. GeckoLib item animations share one
 * model, so the renderer must not guess the owner from stack identity.
 */
public final class GunRenderOwner {
    public record Holder(int entityId, InteractionHand hand) {
    }

    private static final ThreadLocal<Deque<Holder>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private GunRenderOwner() {
    }

    public static void push(LivingEntity entity, boolean leftHand) {
        boolean mainArmIsLeft = entity.getMainArm() == HumanoidArm.LEFT;
        InteractionHand hand = leftHand == mainArmIsLeft ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        STACK.get().push(new Holder(entity.getId(), hand));
    }

    public static void pop() {
        Deque<Holder> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static Holder current() {
        Deque<Holder> stack = STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }
}
