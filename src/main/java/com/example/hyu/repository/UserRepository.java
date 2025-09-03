package com.example.hyu.repository;

import com.example.hyu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    /**
 * Returns whether a User with the given email exists.
 *
 * @param email the email address to check for existence
 * @return {@code true} if a User with the specified email exists; {@code false} otherwise
 */
boolean existsByEmail(String email);
    /**
 * Finds a user by their email address.
 *
 * @param email the email address to look up
 * @return an Optional containing the matching User if present, or Optional.empty() if none found
 */
Optional<User> findByEmail(String email);
}
