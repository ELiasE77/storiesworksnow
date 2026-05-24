package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.user.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "journal_entries",
        indexes = {
                @Index(name = "idx_journal_user_timestamp", columnList = "user_id,timestamp"),
                @Index(name = "idx_journal_visibility_timestamp", columnList = "visibility,timestamp"),
                @Index(name = "idx_journal_community_timestamp", columnList = "community_id,timestamp")
        }
)
public class JournalEntry {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public enum Visibility { PRIVATE, PUBLIC, COMMUNITY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    /**
     * Map to MySQL TEXT rather than a CLOB‐style column,
     * so Hibernate’s validator sees exactly TEXT.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Lob
    @Column(name = "image_url", columnDefinition = "MEDIUMTEXT")
    private String imageUrl;

    @Lob
    @Column(name = "image_urls_json", columnDefinition = "MEDIUMTEXT")
    private String imageUrlsJson;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Lob
    @Column(name = "entry_data_json", columnDefinition = "LONGTEXT")
    private String entryDataJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    // ───── Getters & Setters ───────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageUrlsJson() {
        return imageUrlsJson;
    }

    public void setImageUrlsJson(String imageUrlsJson) {
        this.imageUrlsJson = imageUrlsJson;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getEntryDataJson() {
        return entryDataJson;
    }

    public void setEntryDataJson(String entryDataJson) {
        this.entryDataJson = entryDataJson;
    }

    @Transient
    public List<String> getImageGallery() {
        List<String> images = new ArrayList<>();

        if (imageUrlsJson != null && !imageUrlsJson.isBlank()) {
            try {
                images.addAll(OBJECT_MAPPER.readValue(imageUrlsJson, new TypeReference<List<String>>() {}));
            } catch (Exception ignored) {
            }
        }

        if (imageUrl != null && !imageUrl.isBlank() && !images.contains(imageUrl)) {
            images.add(imageUrl);
        }

        return images;
    }

    @Transient
    public String getDisplayImageUrl() {
        return normalizeImageForDisplay(imageUrl);
    }

    @Transient
    public List<String> getDisplayImageGallery() {
        return getImageGallery().stream()
                .map(this::normalizeImageForDisplay)
                .filter(image -> image != null && !image.isBlank())
                .toList();
    }

    private String normalizeImageForDisplay(String image) {
        if (image == null || image.isBlank()) {
            return null;
        }
        if (image.startsWith("data:image")) {
            return image;
        }
        if (image.startsWith("/uploads/")
                || image.startsWith("/images/")
                || image.startsWith("http://")
                || image.startsWith("https://")) {
            return image;
        }
        return "data:image/png;base64," + image;
    }

    @Transient
    public JournalEntryData getEntryData() {
        if (entryDataJson == null || entryDataJson.isBlank()) {
            return new JournalEntryData();
        }

        try {
            return OBJECT_MAPPER.readValue(entryDataJson, JournalEntryData.class);
        } catch (Exception ignored) {
            return new JournalEntryData();
        }
    }

    public void setEntryData(JournalEntryData entryData) {
        if (entryData == null) {
            this.entryDataJson = null;
            return;
        }

        try {
            this.entryDataJson = OBJECT_MAPPER.writeValueAsString(entryData);
        } catch (Exception ignored) {
            this.entryDataJson = null;
        }
    }

    @Transient
    public int getMoodScore() {
        return getEntryData().getMoodScore() == null ? 4 : getEntryData().getMoodScore();
    }

    @Transient
    public String getQuickNote() {
        return getEntryData().getQuickNote();
    }

    @Transient
    public List<String> getEmotionTags() {
        return getEntryData().getEmotionTags();
    }

    @Transient
    public List<String> getFocusTags() {
        return getEntryData().getFocusTags();
    }

    @Transient
    public List<String> getSupportTags() {
        return getEntryData().getSupportTags();
    }

    @Transient
    public List<EntryReflectionPrompt> getReflectionPrompts() {
        return getEntryData().getReflectionPrompts();
    }

    @Transient
    public String getAiFeedback() {
        return getEntryData().getAiFeedback();
    }

    @Transient
    public Integer getAiSentimentScore() {
        return getEntryData().getAiSentimentScore();
    }

    @Transient
    public String getVoiceMemoTranscript() {
        return getEntryData().getVoiceMemoTranscript();
    }

    @Transient
    public String getVoiceMemoAudioUrl() {
        return getEntryData().getVoiceMemoAudioUrl();
    }

    @Transient
    public String getCustomEmotion() {
        return getEntryData().getCustomEmotion();
    }

    @Transient
    public String getImageStyle() {
        return getEntryData().getImageStyle();
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }
}
