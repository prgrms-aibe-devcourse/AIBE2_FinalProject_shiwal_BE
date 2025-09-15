package com.example.hyu.repository;

import com.example.hyu.entity.ProfileConcern;
import com.example.hyu.enums.ProfileConcernTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileConcernRepository extends JpaRepository<ProfileConcern, ProfileConcern.PK> {
    /**
 * Retrieves all ProfileConcern entries associated with the given user ID.
 *
 * @param userId the ID of the user whose profile concerns should be returned
 * @return a list of ProfileConcern entities for the user; empty if none exist
 */
List<ProfileConcern> findByUserId(Long userId);
    /**
 * Delete all ProfileConcern entities belonging to the given user.
 *
 * Removes every ProfileConcern record whose userId matches the provided identifier.
 *
 * @param userId identifier of the user whose profile concerns will be deleted
 */
void deleteByUserId(Long userId);
    /**
 * Checks whether a ProfileConcern exists for the given user and tag.
 *
 * @param userId the ID of the user to check
 * @param tag the concern tag to check for
 * @return true if at least one ProfileConcern exists for the user with the specified tag, false otherwise
 */
boolean existsByUserIdAndTag(Long userId, ProfileConcernTag tag);
}