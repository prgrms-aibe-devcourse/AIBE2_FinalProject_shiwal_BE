package com.example.hyu.repository;

import com.example.hyu.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);
    Optional<Users> findByEmail(String email);

    //비번 재설정 필요하고, 토큰 아직 만료되지 않은 사용자들만 조회
    List<Users> findAllByNeedPasswordResetTrueAndPasswordResetExpiresAtAfter(Instant now);
}
