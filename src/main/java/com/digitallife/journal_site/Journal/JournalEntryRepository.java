package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /* =========================================================
       BASIC USER QUERIES
       ========================================================= */

    // All entries of a user (newest first)
    List<JournalEntry> findByUserOrderByTimestampDesc(User user);

    // All entries of a user (unsorted, if ever needed)
    List<JournalEntry> findByUser(User user);

    // All entries globally (newest first)
    List<JournalEntry> findAllByOrderByTimestampDesc();


    /* =========================================================
       COMMUNITY QUERIES
       ========================================================= */

    List<JournalEntry> findByCommunityOrderByTimestampDesc(Community community);


    /* =========================================================
       PUBLIC FEED
       ========================================================= */

    List<JournalEntry> findByVisibilityOrderByTimestampDesc(
            JournalEntry.Visibility visibility
    );


    /* =========================================================
       TIMELINE (MONTH / YEAR FILTERING)
       ========================================================= */

    @Query("""
        SELECT e
        FROM JournalEntry e
        WHERE e.user = :user
          AND FUNCTION('MONTH', e.timestamp) = :month
          AND FUNCTION('YEAR', e.timestamp) = :year
        ORDER BY e.timestamp DESC
    """)
    List<JournalEntry> findByUserAndMonthAndYear(
            @Param("user") User user,
            @Param("month") int month,
            @Param("year") int year
    );


    /* =========================================================
       TIMELINE WITH IMAGES (handles single + multiple images)
       ========================================================= */

    @Query("""
        SELECT e
        FROM JournalEntry e
        WHERE e.user = :user
          AND FUNCTION('MONTH', e.timestamp) = :month
          AND FUNCTION('YEAR', e.timestamp) = :year
          AND (
                e.imageUrl IS NOT NULL
                OR e.imageUrls IS NOT EMPTY
              )
        ORDER BY e.timestamp DESC
    """)
    List<JournalEntry> findByUserAndMonthAndYearWithImages(
            @Param("user") User user,
            @Param("month") int month,
            @Param("year") int year
    );


    /* =========================================================
       DISTINCT MONTHS & YEARS (for dropdown selector)
       ========================================================= */

    @Query("""
        SELECT DISTINCT FUNCTION('MONTH', e.timestamp),
                        FUNCTION('YEAR',  e.timestamp)
        FROM JournalEntry e
        WHERE e.user = :user
        ORDER BY FUNCTION('YEAR',  e.timestamp) DESC,
                 FUNCTION('MONTH', e.timestamp) DESC
    """)
    List<Object[]> findDistinctMonthsAndYears(
            @Param("user") User user
    );


    /* =========================================================
       DISTINCT MONTHS & YEARS WITH IMAGES
       ========================================================= */

    @Query("""
        SELECT DISTINCT FUNCTION('MONTH', e.timestamp),
                        FUNCTION('YEAR',  e.timestamp)
        FROM JournalEntry e
        WHERE e.user = :user
          AND (
                e.imageUrl IS NOT NULL
                OR e.imageUrls IS NOT EMPTY
              )
        ORDER BY FUNCTION('YEAR',  e.timestamp) DESC,
                 FUNCTION('MONTH', e.timestamp) DESC
    """)
    List<Object[]> findDistinctMonthsAndYearsWithImages(
            @Param("user") User user
    );

}