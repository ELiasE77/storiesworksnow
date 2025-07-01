package com.digitallife.journal_site.profile;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * Lookup a Profile by the linked User’s username.
     */
    Optional<Profile> findByUserUsername(String username);

    /**
     * (If you ever need it) Lookup a Profile by the User’s id.
     */
    Optional<Profile> findByUserId(Long userId);
}
