package net.exmo.exworld.npc.dialog;

/** A dialog line source. fixed is local; http is the configured remote model. */
public interface DialogModel {
    String complete(DialogRequest request) throws Exception;
}
