package com.digitallife.journal_site.social;

import com.digitallife.journal_site.Journal.JournalEntry;
import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntryLikeRepository extends JpaRepository<EntryLike, Long> {
    long countByEntry(JournalEntry entry);
    boolean existsByEntryAndUser(JournalEntry entry, User user);
    Optional<EntryLike> findByEntryAndUser(JournalEntry entry, User user);
    void deleteByEntryAndUser(JournalEntry entry, User user);
}