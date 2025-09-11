package com.example.hyu.service;

import com.example.hyu.dto.admin.user.ResetConfirmRequest; // 패키지 네가 쓰는 그대로
import com.example.hyu.entity.Users;
import com.example.hyu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // ✅ 발급/확정 둘 다 동일 인코더 사용 권장

    @Transactional
    public void confirm(ResetConfirmRequest req) {
        Instant now = Instant.now();

        // 후보군만 좁혀서 조회(풀스캔 방지)
        Users target = userRepository
                .findAllByNeedPasswordResetTrueAndPasswordResetExpiresAtAfter(now)
                .stream()
                .filter(u -> passwordEncoder.matches(req.token(), u.getPasswordResetTokenHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("TOKEN_INVALID_OR_EXPIRED"));

        // 새 비밀번호 저장 + 토큰/플래그 정리
        target.setPassword(passwordEncoder.encode(req.newPassword()));
        target.setPasswordResetTokenHash(null);
        target.setPasswordResetExpiresAt(null);
        target.setNeedPasswordReset(false);
        // ✅ @Transactional 덕분에 여기서 더티체킹 → 커밋 시 자동 flush/save
    }
}