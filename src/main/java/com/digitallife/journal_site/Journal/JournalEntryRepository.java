package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    // Fetch journal entries for a specific user, ordered by entryDate in descending order
    List<JournalEntry> findByUser(User user);

    // Fetch all journal entries, ordered by timestamp in descending order
    List<JournalEntry> findAllByOrderByTimestampDesc();

    List<JournalEntry> findByCommunityOrderByTimestampDesc(Community community);

    // Find journal entries for a specific user, filtered by month and year, and having images
    @Query("SELECT e FROM JournalEntry e WHERE e.user = :user AND FUNCTION('MONTH', e.timestamp) = :month AND FUNCTION('YEAR', e.timestamp) = :year AND e.image IS NOT NULL ORDER BY e.timestamp DESC")
    List<JournalEntry> findByUserAndMonthAndYearWithImages(@Param("user") User user, @Param("month") int month, @Param("year") int year);

    // Find unique months and years for journal entries for a user
    @Query("SELECT DISTINCT FUNCTION('MONTH', e.timestamp), FUNCTION('YEAR', e.timestamp) FROM JournalEntry e WHERE e.user = :user AND e.image IS NOT NULL ORDER BY FUNCTION('YEAR', e.timestamp) DESC, FUNCTION('MONTH', e.timestamp) DESC")
    List<Object[]> findDistinctMonthsAndYearsWithImages(@Param("user") User user);

    // Query to find only public journal entries and order them by timestamp (descending)
    List<JournalEntry> findByVisibilityOrderByTimestampDesc(JournalEntry.Visibility visibility);

}


