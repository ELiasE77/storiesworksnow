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

    //query to gather all communities from a user from the join table (necessary because of lazy fetch type)
    @Query("SELECT c FROM Community c JOIN c.users u WHERE u.username = :username")
    Set<Community> findByUsername(@Param("username") String username);

    // Search communities by name
    List<Community> findByNameContainingIgnoreCase(String name);
}
