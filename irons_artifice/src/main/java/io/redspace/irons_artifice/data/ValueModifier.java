package io.redspace.irons_artifice.data;

public record ValueModifier(double amount, Operation operation, Type type) {
    public enum Operation {
        ADD,
        MULTIPLY_TOTAL,
        ;
    }

    public enum Type {
        BENEFICIAL,
        HARMFUL,
        NEUTRAL,
        ;
    }
}
