package net.exmo.exworld.npc.data;

public record TradeSpec(String payItem, int payCount, String resultItem, int resultCount, int maxUses) {
    public TradeSpec {
        payItem = payItem == null ? "" : payItem.trim();
        resultItem = resultItem == null ? "" : resultItem.trim();
        payCount = Math.max(1, payCount);
        resultCount = Math.max(1, resultCount);
        maxUses = Math.max(1, maxUses);
    }
}
