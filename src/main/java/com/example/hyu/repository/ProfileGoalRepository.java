package com.example.hyu.repository;

import com.example.hyu.entity.ProfileGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileGoalRepository extends JpaRepository<ProfileGoal, Long> {
    /**
 * Retrieves all ProfileGoal entities for the given user, ordered by `orderNo` ascending then `id` ascending.
 *
 * @param userId the id of the user whose ProfileGoal records should be returned
 * @return a list of ProfileGoal entities matching the user, ordered by orderNo ASC, then id ASC
 */
List<ProfileGoal> findByUserIdOrderByOrderNoAscIdAsc(Long userId);
    /**
 * Delete all ProfileGoal entities belonging to the specified user.
 *
 * @param userId the ID of the user whose ProfileGoal records should be removed
 */
void deleteByUserId(Long userId);
}
