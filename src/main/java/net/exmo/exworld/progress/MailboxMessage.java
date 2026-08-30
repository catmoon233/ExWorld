package net.exmo.exworld.progress;

import java.time.Instant;
import java.util.*;
import net.minecraft.world.item.ItemStack;

/** A permanent player mail item. Attachments are retained until actually placed in inventory. */
public final class MailboxMessage {
    private final UUID id; private final String source; private final String subject; private final Instant deliveredAt;
    private final List<ItemStack> attachments; private boolean read;
    public MailboxMessage(UUID id, String source, String subject, Instant deliveredAt, Collection<ItemStack> attachments, boolean read) {
        this.id = id; this.source = source; this.subject = subject; this.deliveredAt = deliveredAt; this.attachments = new ArrayList<>();
        attachments.forEach(stack -> { if (!stack.isEmpty()) this.attachments.add(stack.copy()); }); this.read = read;
    }
    public UUID id() { return id; } public String source() { return source; } public String subject() { return subject; }
    public Instant deliveredAt() { return deliveredAt; } public List<ItemStack> attachments() { return attachments; }
    public boolean read() { return read; } public void read(boolean value) { read = value; }
}
