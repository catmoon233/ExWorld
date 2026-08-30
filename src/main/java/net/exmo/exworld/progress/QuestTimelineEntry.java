package net.exmo.exworld.progress;

import java.time.Instant;
import net.minecraft.resources.ResourceLocation;

/** Immutable audit event, also used by the journal history page. */
public record QuestTimelineEntry(Instant at, long gameTime, ResourceLocation questId, String type, String detail) {
    public QuestTimelineEntry { detail = detail == null ? "" : detail; }
}
