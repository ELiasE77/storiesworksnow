package com.digitallife.journal_site.reflection;

import com.digitallife.journal_site.Journal.JournalEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ReflectionMessageRepository extends JpaRepository<ReflectionMessage, Long> {
    List<ReflectionMessage> findByEntryOrderByTimestampAsc(JournalEntry entry);

    List<ReflectionMessage> findByEntryUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    Optional<ReflectionMessage> findByIdAndEntryId(Long id, Long entryId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ReflectionMessage m WHERE m.entry = :entry AND m.id > :messageId")
    void deleteMessagesAfter(@Param("entry") JournalEntry entry, @Param("messageId") Long messageId);
}