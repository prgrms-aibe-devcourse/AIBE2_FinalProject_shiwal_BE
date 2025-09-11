package com.example.hyu.dto.admin.user;

import java.time.Instant;
import com.example.hyu.entity.Users;

public record UserSummaryResponse(
        Long id,
        String name,       // 이름
        String nickname,   // 닉네임
        String email,      // 이메일
        String role,  // USER / ADMIN
        String state,
        Instant joinedAt   // 가입일
) {}