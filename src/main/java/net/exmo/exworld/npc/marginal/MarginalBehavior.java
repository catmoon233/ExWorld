package net.exmo.exworld.npc.marginal;

import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.logic.BrainContext;

/** A temporary reaction. Implementations must treat missing targets as not triggered. */
public interface MarginalBehavior {
    String id();

    boolean triggered(BrainContext context, MarginalBinding binding);
}
