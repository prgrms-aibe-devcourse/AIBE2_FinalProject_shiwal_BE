package com.example.hyu.repository;

import com.example.hyu.entity.ProfileGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileGoalRepository extends JpaRepository<ProfileGoal, Long> {
    List<ProfileGoal> findByUserIdOrderByOrderNoAscIdAsc(Long userId);
    void deleteByUserId(Long userId);
}
