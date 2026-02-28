package com.digitallife.journal_site.Journal;

import java.util.List;

public class JournalEntryDTO {
    private String title;
    private String content;
    private String imageUrl;
    private List<String> imageUrls;
    private Long communityId;
    private JournalEntry.Visibility visibility;

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
}