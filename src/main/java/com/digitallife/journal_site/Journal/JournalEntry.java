package com.digitallife.journal_site.Journal;


import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "image", columnDefinition = "MEDIUMTEXT") //definition MEDIUMTEXT is needed since base64 Strings are very large so MySql database has them listed as MEDIUMTEXT
    private String image;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; //specifies which user wrote the entry (FK)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility; //enumeration can be seen below

    @ManyToOne
    @JoinColumn(name = "community_id")
    private Community community; //specifies in which community it was posted (FK)

    // Enum for visibility used to specify who can see your entries
    public enum Visibility {
        PRIVATE,
        PUBLIC,
        COMMUNITY
    }
    // ************************************ Getters and setters ********************************************************
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community community) {
        this.community = community;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String imageUrl) {
        this.image = imageUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}




