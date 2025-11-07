package com.digitallife.journal_site.Journal;

import java.time.LocalDateTime;

/**
 * Lightweight projection for listing journal entries without eagerly
 * loading potentially large image payloads. The template can render the
 * textual data immediately and request the image separately if needed.
 */
public class JournalEntrySummary {

    private final Long id;
    private final String title;
    private final String content;
    private final LocalDateTime timestamp;
    private final String username;
    private final JournalEntry.Visibility visibility;
    private final boolean hasImage;

    public JournalEntrySummary(
            Long id,
            String title,
            String content,
            LocalDateTime timestamp,
            String username,
            JournalEntry.Visibility visibility,
            boolean hasImage
    ) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.username = username;
        this.visibility = visibility;
        this.hasImage = hasImage;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public JournalEntry.Visibility getVisibility() {
        return visibility;
    }

    public boolean isHasImage() {
        return hasImage;
    }
}