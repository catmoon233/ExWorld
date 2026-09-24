package net.exmo.exworld.npc.data;

import java.util.List;

public record DialogScript(
        String id,
        DialogMode mode,
        List<String> lines,
        List<DialogButton> buttons,
        DialogBackground background,
        String prompt,
        String fallback,
        int typeSpeed) {
    public DialogScript {
        id = id == null ? "" : id.trim();
        mode = mode == null ? DialogMode.FIXED : mode;
        lines = lines == null ? List.of() : List.copyOf(lines);
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
        background = background == null ? DialogBackground.DEFAULT : background;
        prompt = prompt == null ? "" : prompt;
        fallback = fallback == null ? "" : fallback;
        typeSpeed = Math.max(1, Math.min(typeSpeed, 8));
    }

    public String primaryLine() {
        for (String line : lines) if (line != null && !line.isBlank()) return line;
        return fallback == null ? "" : fallback;
    }

    public String fallbackOrLine() {
        if (fallback != null && !fallback.isBlank()) return fallback;
        return primaryLine();
    }

    public enum DialogMode {
        FIXED,
        AI;

        public static DialogMode parse(String raw) {
            return raw != null && raw.equalsIgnoreCase("AI") ? AI : FIXED;
        }
    }

    public record DialogButton(String label, String dialogId, String actionId) {
        public DialogButton {
            label = label == null ? "" : label;
            dialogId = dialogId == null ? "" : dialogId.trim();
            actionId = actionId == null ? "" : actionId.trim();
        }
    }
}
