package net.exmo.exworld.client.tooltip;

import java.util.List;

public final class VanillaLineFilterTestHarness {
    public static void main(String[] args) {
        List<VanillaLineFilter.Line> lines = List.of(
                new VanillaLineFilter.Line("item.minecraft.iron_sword", "Iron Sword"),
                new VanillaLineFilter.Line("tooltip.exmodifier.applied_entry", "Entry sharp Lv.2"),
                new VanillaLineFilter.Line("tooltip.exmodifier.quality", "Quality: common"),
                new VanillaLineFilter.Line("", ""),
                new VanillaLineFilter.Line("enchantment.minecraft.sharpness", "Sharpness V"),
                new VanillaLineFilter.Line("attribute.modifier.equals.0", "+7 Attack Damage")
        );
        List<VanillaLineFilter.Line> body = VanillaLineFilter.body(lines);
        if (body.size() != 2) throw new AssertionError("expected 2 kept lines, got " + body.size());
        if (!"Sharpness V".equals(body.get(0).text())) throw new AssertionError("enchantment line should remain");
        if (!"+7 Attack Damage".equals(body.get(1).text())) throw new AssertionError("attribute line should remain");
        for (VanillaLineFilter.Line line : body) {
            if (line.exModifierDump()) throw new AssertionError("exmodifier dump keys must be stripped");
        }
        System.out.println("Vanilla line filter tests passed");
    }
}
