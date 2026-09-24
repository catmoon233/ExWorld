package net.exmo.exworld.npc.dialog;

import net.exmo.exworld.npc.data.DialogScript;

/** Resolves a script without blocking on a failed model. AI failure always returns the fallback line. */
public final class DialogResolver {
    private DialogResolver() {}

    public static String resolve(DialogScript script, DialogModel model, DialogRequest request) {
        if (script == null) return "";
        if (script.mode() != DialogScript.DialogMode.AI) return script.primaryLine();
        try {
            if (model == null) return script.fallbackOrLine();
            String value = model.complete(request);
            if (value == null || value.isBlank()) return script.fallbackOrLine();
            return value.trim();
        } catch (Exception ex) {
            return script.fallbackOrLine();
        }
    }
}
