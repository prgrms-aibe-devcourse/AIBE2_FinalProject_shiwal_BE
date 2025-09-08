package com.example.hyu.repository;

import com.example.hyu.entity.ProfileConcern;
import com.example.hyu.enums.ProfileConcernTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileConcernRepository extends JpaRepository<ProfileConcern, ProfileConcern.PK> {
    List<ProfileConcern> findByUserId(Long userId);
    void deleteByUserId(Long userId);
    boolean existsByUserIdAndTag(Long userId, ProfileConcernTag tag);
}