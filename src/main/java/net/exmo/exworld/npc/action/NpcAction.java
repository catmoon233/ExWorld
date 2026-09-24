package net.exmo.exworld.npc.action;

/** A registered action type. Documents store ids; the registry decides which types exist. */
public interface NpcAction {
    String type();

    boolean needsPlace();

    boolean needsRoute();
}
