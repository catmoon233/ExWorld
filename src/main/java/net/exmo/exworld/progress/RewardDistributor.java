package net.exmo.exworld.progress;

import java.time.Instant;
import java.util.*;
import net.exmo.exworld.battle.BattleSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Centralizes all reward delivery and turns every unfinished item insertion into durable mail. */
public final class RewardDistributor {
    private final PlayerResourceVault vault; private final QuestModule quests;
    public RewardDistributor(PlayerResourceVault vault, QuestModule quests) { this.vault = vault; this.quests = quests; }
    public void deliver(ServerPlayer player, String source, String subject, Collection<QuestReward> rewards) {
        List<ItemStack> overflow = new ArrayList<>(); int index = 0;
        for (QuestReward reward : rewards) { String id = source + ":" + index++;
            switch (reward.kind()) {
                case RESOURCE -> vault.awardOnce(player.getServer(), player.getUUID(), id, reward.id(), reward.amount());
                case EXPERIENCE -> { if (vault.awardOnce(player.getServer(), player.getUUID(), id, PlayerResourceVault.GOLD, 0)) player.giveExperiencePoints(reward.amount()); }
                case CARD -> { if (vault.awardOnce(player.getServer(), player.getUUID(), id, PlayerResourceVault.GOLD, 0)) BattleSystem.playerCards().grant(player.getServer(), player.getUUID(), reward.id().toString(), reward.amount()); }
                case ACTION -> { if (vault.awardOnce(player.getServer(), player.getUUID(), id, PlayerResourceVault.GOLD, 0) && reward.id().getNamespace().equals("exworld") && reward.id().getPath().equals("grant_quest")) quests.grant(player, ResourceLocation.parse(reward.actionValue()), "reward:" + source); }
                case ITEM -> { if (!vault.awardOnce(player.getServer(), player.getUUID(), id, PlayerResourceVault.GOLD, 0)) continue; var item = BuiltInRegistries.ITEM.get(reward.id()); ItemStack stack = new ItemStack(item, reward.amount()); if (!player.getInventory().add(stack)) overflow.add(stack.copy()); }
            }
        }
        if (!overflow.isEmpty()) mail(player, source, subject, overflow);
    }
    public void mail(ServerPlayer player, String source, String subject, Collection<ItemStack> attachments) {
        PlayerProgress state = vault.state(player.getServer(), player.getUUID()); state.mailbox.add(new MailboxMessage(UUID.randomUUID(), source, subject, Instant.now(), attachments, false)); vault.saved(player.getServer()).changed();
    }
    public boolean claim(ServerPlayer player, UUID messageId) {
        PlayerProgress state = vault.state(player.getServer(), player.getUUID()); MailboxMessage message = state.mailbox.stream().filter(value -> value.id().equals(messageId)).findFirst().orElse(null); if (message == null) return false;
        Iterator<ItemStack> it = message.attachments().iterator(); while (it.hasNext()) { ItemStack stack = it.next(); if (player.getInventory().add(stack)) it.remove(); }
        message.read(true); if (message.attachments().isEmpty()) state.mailbox.remove(message); vault.saved(player.getServer()).changed(); return true;
    }
}
