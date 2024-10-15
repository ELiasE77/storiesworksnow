package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    // Fetch journal entries for a specific user, ordered by entryDate in descending order
    List<JournalEntry> findByUser(User user);
}


