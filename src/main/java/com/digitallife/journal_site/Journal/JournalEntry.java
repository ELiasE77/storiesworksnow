package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Optional;

@Entity
public class JournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private LocalDate entryDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public JournalEntry() {
        this.entryDate = LocalDate.now();
    }

    public JournalEntry(String content, User user) {
        this.content = content;
        this.user = user;
        this.entryDate = LocalDate.now();
    }

    // ************************************* Getters and setters ******************************************************

    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    public void setUser(Optional<User> user) {
        this.user = user.orElse(null);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }
}



