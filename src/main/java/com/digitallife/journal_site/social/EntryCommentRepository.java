package com.digitallife.journal_site.social;

import com.digitallife.journal_site.Journal.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntryCommentRepository extends JpaRepository<EntryComment, Long> {
    List<EntryComment> findByEntryOrderByTimestampAsc(JournalEntry entry);
}