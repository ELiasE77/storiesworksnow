package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.Journal.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReflectionMessageRepository extends JpaRepository<ReflectionMessage, Long> {
    List<ReflectionMessage> findByEntryOrderByTimestampAsc(JournalEntry entry);
}