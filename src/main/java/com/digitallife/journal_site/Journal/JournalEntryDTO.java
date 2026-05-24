package com.digitallife.journal_site.Journal;

import java.util.List;

public class JournalEntryDTO {
    private String title;
    private String content;
    private String imageUrl;
    private List<String> imageUrls;
    private String voiceMemoAudioDataUrl;
    private Long communityId;
    private JournalEntry.Visibility visibility;
    private JournalEntryData entryData;

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public List<String> getImageUrls() {
        return imageUrls;
    }
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    public String getVoiceMemoAudioDataUrl() {
        return voiceMemoAudioDataUrl;
    }
    public void setVoiceMemoAudioDataUrl(String voiceMemoAudioDataUrl) {
        this.voiceMemoAudioDataUrl = voiceMemoAudioDataUrl;
    }
    public Long getCommunityId() {
        return communityId;
    }
    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }
    public JournalEntry.Visibility getVisibility() {
        return visibility;
    }
    public void setVisibility(JournalEntry.Visibility visibility) {
        this.visibility = visibility;
    }

    public JournalEntryData getEntryData() {
        return entryData;
    }

    public void setEntryData(JournalEntryData entryData) {
        this.entryData = entryData;
    }
}
