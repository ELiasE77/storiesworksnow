package com.digitallife.journal_site.communities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CommunityRepository extends JpaRepository<Community, Long> {
    //get the community based on the id provided
    Optional<Community> findById(Long id);

    //query to gather all communities from a user from the join table (necessary to use querying because of lazy fetch type)
    @Query("SELECT c FROM Community c JOIN c.users u WHERE u.username = :username")
    Set<Community> findByUsername(@Param("username") String username);

    @Query("""
        SELECT c.id AS id,
               c.name AS name,
               c.description AS description,
               COUNT(member) AS memberCount
        FROM Community c
        LEFT JOIN c.users member
        GROUP BY c.id, c.name, c.description
        ORDER BY c.name ASC
    """)
    List<CommunitySummaryProjection> findAllSummaries();

    @Query("""
        SELECT c.id AS id,
               c.name AS name,
               c.description AS description,
               COUNT(member) AS memberCount
        FROM Community c
        JOIN c.users currentUser
        LEFT JOIN c.users member
        WHERE currentUser.username = :username
        GROUP BY c.id, c.name, c.description
        ORDER BY c.name ASC
    """)
    List<CommunitySummaryProjection> findSummariesByUsername(@Param("username") String username);

    //find all communities in the database (be careful when the database starts growing)
    List<Community> findAll();
}
