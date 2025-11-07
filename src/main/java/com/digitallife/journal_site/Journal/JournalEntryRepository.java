package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByUser(User user);

    @Query("""
      SELECT new com.digitallife.journal_site.Journal.JournalEntrySummary(
             e.id,
             e.title,
             e.content,
             e.timestamp,
             e.user.username,
             e.visibility,
             CASE WHEN e.imageUrl IS NOT NULL THEN true ELSE false END)
        FROM JournalEntry e
       WHERE e.user = :user
    ORDER BY e.timestamp DESC
    """)
    List<JournalEntrySummary> findSummariesByUserOrderByTimestampDesc(@Param("user") User user);

    @Query("""
      SELECT e.imageUrl
        FROM JournalEntry e
       WHERE e.id = :id
    """)
    Optional<String> findImageById(@Param("id") Long id);

    List<JournalEntry> findAllByOrderByTimestampDesc();

    List<JournalEntry> findByCommunityOrderByTimestampDesc(Community community);

    @Query("""
      SELECT e
        FROM JournalEntry e
       WHERE e.user       = :user
         AND FUNCTION('MONTH', e.timestamp) = :month
         AND FUNCTION('YEAR',  e.timestamp) = :year
         AND e.imageUrl   IS NOT NULL
    ORDER BY e.timestamp DESC
    """)
    List<JournalEntry> findByUserAndMonthAndYearWithImages(
            @Param("user") User user,
            @Param("month") int month,
            @Param("year") int year
    );

    @Query("""
      SELECT DISTINCT FUNCTION('MONTH', e.timestamp), FUNCTION('YEAR', e.timestamp)
        FROM JournalEntry e
       WHERE e.user     = :user
         AND e.imageUrl IS NOT NULL
    ORDER BY FUNCTION('YEAR',  e.timestamp) DESC,
             FUNCTION('MONTH', e.timestamp) DESC
    """)
    List<Object[]> findDistinctMonthsAndYearsWithImages(@Param("user") User user);

    List<JournalEntry> findByVisibilityOrderByTimestampDesc(JournalEntry.Visibility visibility);
}
