package net.exmo.lotm.sequence;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Lord of the Mysteries ladder. Lower sequence numbers are stronger.
 * Above sequence 0 comes Old One, then Pillar.
 */
public enum SequenceRank {
    SEQUENCE_9(9, "9", "sequence.exworld.rank.9"),
    SEQUENCE_8(8, "8", "sequence.exworld.rank.8"),
    SEQUENCE_7(7, "7", "sequence.exworld.rank.7"),
    SEQUENCE_6(6, "6", "sequence.exworld.rank.6"),
    SEQUENCE_5(5, "5", "sequence.exworld.rank.5"),
    SEQUENCE_4(4, "4", "sequence.exworld.rank.4"),
    SEQUENCE_3(3, "3", "sequence.exworld.rank.3"),
    SEQUENCE_2(2, "2", "sequence.exworld.rank.2"),
    SEQUENCE_1(1, "1", "sequence.exworld.rank.1"),
    SEQUENCE_0(0, "0", "sequence.exworld.rank.0"),
    OLD_ONE(-1, "old_one", "sequence.exworld.rank.old_one"),
    PILLAR(-2, "pillar", "sequence.exworld.rank.pillar");

    private final int number;
    private final String token;
    private final String translationKey;

    SequenceRank(int number, String token, String translationKey) {
        this.number = number;
        this.token = token;
        this.translationKey = translationKey;
    }

    /** Sequence number, or -1 for Old One and -2 for Pillar. */
    public int number() {
        return number;
    }

    public String token() {
        return token;
    }

    public String translationKey() {
        return translationKey;
    }

    /** 0 is sequence 9. Higher power is a higher rank. */
    public int power() {
        return ordinal();
    }

    public boolean unlockedWhenCurrentIs(SequenceRank current) {
        return current != null && power() <= current.power();
    }

    public static Optional<SequenceRank> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String token = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (token) {
            case "9", "seq9", "sequence9", "sequence_9" -> Optional.of(SEQUENCE_9);
            case "8", "seq8", "sequence8", "sequence_8" -> Optional.of(SEQUENCE_8);
            case "7", "seq7", "sequence7", "sequence_7" -> Optional.of(SEQUENCE_7);
            case "6", "seq6", "sequence6", "sequence_6" -> Optional.of(SEQUENCE_6);
            case "5", "seq5", "sequence5", "sequence_5" -> Optional.of(SEQUENCE_5);
            case "4", "seq4", "sequence4", "sequence_4" -> Optional.of(SEQUENCE_4);
            case "3", "seq3", "sequence3", "sequence_3" -> Optional.of(SEQUENCE_3);
            case "2", "seq2", "sequence2", "sequence_2" -> Optional.of(SEQUENCE_2);
            case "1", "seq1", "sequence1", "sequence_1" -> Optional.of(SEQUENCE_1);
            case "0", "seq0", "sequence0", "sequence_0" -> Optional.of(SEQUENCE_0);
            case "old_one", "oldone", "great_old_one", "旧日" -> Optional.of(OLD_ONE);
            case "pillar", "支柱" -> Optional.of(PILLAR);
            default -> Arrays.stream(values()).filter(rank -> rank.token.equals(token)).findFirst();
        };
    }

    public static String[] tokens() {
        return Arrays.stream(values()).map(SequenceRank::token).toArray(String[]::new);
    }
}
