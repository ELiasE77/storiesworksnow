package com.digitallife.journal_site.Journal;

import com.digitallife.journal_site.communities.Community;
import com.digitallife.journal_site.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /* =========================================================
       BASIC USER QUERIES
       ========================================================= */

    // All entries of a user (newest first)
    List<JournalEntry> findByUserOrderByTimestampDesc(User user);

    JournalEntry findFirstByUserOrderByTimestampDesc(User user);

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

    long countByUser(User user);

    @Query("""
        SELECT e.id AS id,
               e.title AS title,
               SUBSTRING(e.content, 1, 420) AS excerpt,
               e.timestamp AS timestamp,
               e.visibility AS visibility,
               e.thumbnailUrl AS thumbnailUrl,
               CASE WHEN e.thumbnailUrl IS NOT NULL OR e.imageUrl IS NOT NULL OR e.imageUrlsJson IS NOT NULL THEN true ELSE false END AS hasImage,
               e.entryDataJson AS entryDataJson,
               u.username AS username
        FROM JournalEntry e
        JOIN e.user u
        WHERE e.user = :user
          AND e.timestamp >= :start
          AND e.timestamp < :end
        ORDER BY e.timestamp DESC
    """)
    List<JournalEntryListProjection> findListItemsByUserAndTimestampRange(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT e.id AS id,
               e.title AS title,
               SUBSTRING(e.content, 1, 420) AS excerpt,
               e.timestamp AS timestamp,
               e.visibility AS visibility,
               e.thumbnailUrl AS thumbnailUrl,
               CASE WHEN e.thumbnailUrl IS NOT NULL OR e.imageUrl IS NOT NULL OR e.imageUrlsJson IS NOT NULL THEN true ELSE false END AS hasImage,
               e.entryDataJson AS entryDataJson,
               u.username AS username
        FROM JournalEntry e
        JOIN e.user u
        WHERE e.user = :user
        ORDER BY e.timestamp DESC
    """)
    List<JournalEntryListProjection> findRecentListItemsByUser(
            @Param("user") User user,
            Pageable pageable
    );

    @Query("""
        SELECT e.id AS id,
               e.title AS title,
               SUBSTRING(e.content, 1, 420) AS excerpt,
               e.timestamp AS timestamp,
               e.visibility AS visibility,
               e.thumbnailUrl AS thumbnailUrl,
               CASE WHEN e.thumbnailUrl IS NOT NULL OR e.imageUrl IS NOT NULL OR e.imageUrlsJson IS NOT NULL THEN true ELSE false END AS hasImage,
               e.entryDataJson AS entryDataJson,
               u.username AS username
        FROM JournalEntry e
        JOIN e.user u
        WHERE e.visibility = :visibility
        ORDER BY e.timestamp DESC
    """)
    List<JournalEntryListProjection> findPublicListItems(
            @Param("visibility") JournalEntry.Visibility visibility,
            Pageable pageable
    );

    @Query("""
        SELECT e.id AS id,
               e.title AS title,
               SUBSTRING(e.content, 1, 420) AS excerpt,
               e.timestamp AS timestamp,
               e.visibility AS visibility,
               e.thumbnailUrl AS thumbnailUrl,
               CASE WHEN e.thumbnailUrl IS NOT NULL OR e.imageUrl IS NOT NULL OR e.imageUrlsJson IS NOT NULL THEN true ELSE false END AS hasImage,
               e.entryDataJson AS entryDataJson,
               u.username AS username
        FROM JournalEntry e
        JOIN e.user u
        WHERE e.community = :community
        ORDER BY e.timestamp DESC
    """)
    List<JournalEntryListProjection> findCommunityListItems(
            @Param("community") Community community,
            Pageable pageable
    );

    @Query("""
        SELECT MAX(e.timestamp)
        FROM JournalEntry e
        WHERE e.user = :user
    """)
    LocalDateTime findLatestTimestampByUser(@Param("user") User user);

    @Query("""
        SELECT e.timestamp
        FROM JournalEntry e
        WHERE e.user = :user
        ORDER BY e.timestamp DESC
    """)
    List<LocalDateTime> findTimestampsByUser(@Param("user") User user);

    @Query("""
        SELECT e.timestamp AS timestamp,
               e.content AS content,
               e.entryDataJson AS entryDataJson,
               e.thumbnailUrl AS thumbnailUrl
        FROM JournalEntry e
        WHERE e.user = :user
        ORDER BY e.timestamp DESC
    """)
    List<JournalInsightProjection> findInsightItemsByUser(@Param("user") User user);


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
       TIMELINE WITH IMAGES (SAFE: only checks existing field)
       ========================================================= */

    @Query("""
        SELECT e
        FROM JournalEntry e
        WHERE e.user = :user
          AND FUNCTION('MONTH', e.timestamp) = :month
          AND FUNCTION('YEAR', e.timestamp) = :year
          AND e.imageUrl IS NOT NULL
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
       DISTINCT MONTHS & YEARS WITH IMAGES (SAFE)
       ========================================================= */

    @Query("""
        SELECT DISTINCT FUNCTION('MONTH', e.timestamp),
                        FUNCTION('YEAR',  e.timestamp)
        FROM JournalEntry e
        WHERE e.user = :user
          AND e.imageUrl IS NOT NULL
        ORDER BY FUNCTION('YEAR',  e.timestamp) DESC,
                 FUNCTION('MONTH', e.timestamp) DESC
    """)
    List<Object[]> findDistinctMonthsAndYearsWithImages(
            @Param("user") User user
    );

}
