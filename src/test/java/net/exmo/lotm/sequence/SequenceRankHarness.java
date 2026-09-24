package net.exmo.lotm.sequence;

public final class SequenceRankHarness {
    private SequenceRankHarness() {}

    public static void main(String[] args) {
        check(SequenceRank.SEQUENCE_9.power() < SequenceRank.SEQUENCE_7.power(), "9 is below 7");
        check(SequenceRank.SEQUENCE_0.power() < SequenceRank.OLD_ONE.power(), "0 is below old one");
        check(SequenceRank.OLD_ONE.power() < SequenceRank.PILLAR.power(), "old one is below pillar");
        check(SequenceRank.SEQUENCE_9.unlockedWhenCurrentIs(SequenceRank.SEQUENCE_7), "9 unlocked at 7");
        check(SequenceRank.SEQUENCE_7.unlockedWhenCurrentIs(SequenceRank.SEQUENCE_7), "7 unlocked at 7");
        check(!SequenceRank.SEQUENCE_6.unlockedWhenCurrentIs(SequenceRank.SEQUENCE_7), "6 locked at 7");
        check(SequenceRank.SEQUENCE_0.unlockedWhenCurrentIs(SequenceRank.OLD_ONE), "0 unlocked at old one");
        check(!SequenceRank.OLD_ONE.unlockedWhenCurrentIs(SequenceRank.SEQUENCE_0), "old one locked at 0");
        check(SequenceRank.parse("7").orElse(null) == SequenceRank.SEQUENCE_7, "parse 7");
        check(SequenceRank.parse("old_one").orElse(null) == SequenceRank.OLD_ONE, "parse old_one");
        check(SequenceRank.parse("支柱").orElse(null) == SequenceRank.PILLAR, "parse pillar");
        check(SequenceRank.parse("nope").isEmpty(), "reject unknown");
        check(SequenceRank.values().length == 12, "9 through 0, old one, pillar");
        System.out.println("SequenceRankHarness ok");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
