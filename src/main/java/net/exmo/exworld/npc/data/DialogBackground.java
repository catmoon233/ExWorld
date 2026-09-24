package net.exmo.exworld.npc.data;

public record DialogBackground(int argb, float alpha, String texture) {
    public static final DialogBackground DEFAULT = new DialogBackground(0xFF101418, 0.86f, "");

    public DialogBackground {
        alpha = Math.max(0f, Math.min(1f, alpha));
        texture = texture == null ? "" : texture.trim();
    }

    public int withAlpha() {
        int a = Math.round(alpha * ((argb >>> 24) & 0xFF));
        if (a <= 0) a = Math.round(alpha * 255f);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
