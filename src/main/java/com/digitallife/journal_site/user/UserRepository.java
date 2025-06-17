package com.digitallife.journal_site.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the User if found, or empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Check whether a user with the given username exists.
     *
     * @param username the username to check
     * @return true if a user exists with that username, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Retrieve all users with a specific role.
     *
     * @param role the role to filter by
     * @return list of users matching the given role
     */
    List<User> findAllByRole(String role);
}
