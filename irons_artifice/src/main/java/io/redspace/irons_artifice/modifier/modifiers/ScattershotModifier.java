package io.redspace.irons_artifice.modifier.modifiers;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.modifier.ValueStackModifier;

import java.util.Map;

public final class ScattershotModifier extends ValueStackModifier {
    public ScattershotModifier() {
        super(Map.of(
                ShotComponents.PROJECTILE_COUNT, new ValueModifier(3, ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL),
                ShotComponents.DAMAGE, new ValueModifier(0.25, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL),
                ShotComponents.SPREAD, new ValueModifier(3, ValueModifier.Operation.ADD, ValueModifier.Type.HARMFUL)
        ));
    }
}
