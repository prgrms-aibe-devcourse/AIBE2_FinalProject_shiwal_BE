package com.example.hyu.service;

import com.example.hyu.dto.admin.user.*;
import com.example.hyu.entity.Users;
import com.example.hyu.repository.AdminUserQueryRepository;
import com.example.hyu.repository.UserRepository;
import com.example.hyu.support.MailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserQueryRepository queryRepository;
    private final UserRepository userRepository;
    private final MailSender mailSender; // 콘솔 스텁 가능(아래 참고)

    /* 목록 + 필터 */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> list(UserSearchCond cond, Pageable pageable) {
        return queryRepository.search(cond, pageable)
                .map(u -> new UserSummaryResponse(
                        u.getId(), u.getName(), u.getNickname(), u.getEmail(),
                        u.getRole(),
                        u.getState() != null ? u.getState().name() : null,
                        u.getCreatedAt()
                ));
    }

    /* 상태 변경 (토글/모달 공용) */
    @Transactional
    public UserSummaryResponse changeState(Long userId, ChangeStateRequest req) {
        Users u = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("USER_NOT_FOUND"));

        //상태값 검증
        Users.UserState next;
        try{
            next = Users.UserState.valueOf(req.state());
        }catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid state: " + req.state());
        }

        if (next == Users.UserState.SUSPENDED) {
            u.setState(Users.UserState.SUSPENDED);
            u.setSuspendUntil(calcUntil(req.period()));     // P1W | P2W | P1M | P100Y (null 허용)
        } else {
            u.setState(Users.UserState.ACTIVE);
            u.setSuspendUntil(null);
        }

        if (req.risk() != null && !req.risk().isBlank()) {
            u.setRiskMode(Users.RiskMode.valueOf(req.risk())); // WARN | EXEMPT
        }

        // TODO(선택): 정지 시 세션/리프레시 토큰 무효화

        return new UserSummaryResponse(
                u.getId(), u.getName(), u.getNickname(), u.getEmail(),
                u.getRole(), u.getState().name(), u.getCreatedAt()
        );
    }

    private Instant calcUntil(String iso) {
        if (iso == null || iso.isBlank()) return null; // 기간 미설정 시 수동 해제 전까지
        return switch (iso) {
            case "P1W"   -> Instant.now().plus(7, ChronoUnit.DAYS);
            case "P2W"   -> Instant.now().plus(14, ChronoUnit.DAYS);
            case "P1M"   -> Instant.now().plus(30, ChronoUnit.DAYS);
            case "P100Y" -> Instant.now().plus(100, ChronoUnit.YEARS); // 사실상 영구
            default      -> throw new IllegalArgumentException("INVALID_PERIOD");
        };
    }

    /* 비번 재설정 링크 발급 */
    // src/main/java/com/example/hyu/service/AdminUserService.java
    @Transactional
    public PasswordResetIssueResponse issuePasswordReset(Long userId) {
        Users u = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("USER_NOT_FOUND"));

        Instant now = Instant.now();

        // ------------------------------
        // 0) 계정 상태 보호: 탈퇴/정지 계정엔 발급 막기 (선택)
        if (u.getState() == Users.UserState.WITHDRAWN) {
            throw new IllegalStateException("ACCOUNT_WITHDRAWN");
        }
        if (u.getState() == Users.UserState.SUSPENDED) {
            // 정지 해제 예정인데 아직이면 막기
            if (u.getSuspendUntil() == null || u.getSuspendUntil().isAfter(now)) {
                throw new IllegalStateException("ACCOUNT_SUSPENDED");
            }
        }

        // ------------------------------
        // 1) 중복 발급 차단: 유효한 토큰이 이미 존재하면 거절
        if (Boolean.TRUE.equals(u.isNeedPasswordReset())
                && u.getPasswordResetExpiresAt() != null
                && u.getPasswordResetExpiresAt().isAfter(now)) {
            long secLeft = u.getPasswordResetExpiresAt().getEpochSecond() - now.getEpochSecond();
            throw new IllegalStateException("RESET_ALREADY_ISSUED:" + Math.max(secLeft, 0));
        }

        // ------------------------------
        // 2) 레이트리밋(쿨다운 5분): 최근 발급 후 5분 이내면 거절
        // issuedAt = expiresAt - TOKEN_TTL(30분)
        final long TOKEN_TTL_SEC = 30 * 60;
        final long COOLDOWN_SEC  = 5 * 60;

        if (u.getPasswordResetExpiresAt() != null) {
            long issuedAtEpoch = u.getPasswordResetExpiresAt().getEpochSecond() - TOKEN_TTL_SEC;
            long sinceIssueSec = now.getEpochSecond() - issuedAtEpoch;
            if (sinceIssueSec >= 0 && sinceIssueSec < COOLDOWN_SEC) {
                long waitSec = COOLDOWN_SEC - sinceIssueSec;
                throw new IllegalStateException("TOO_FREQUENT_RESET_REQUEST:" + waitSec);
            }
        }

        // ------------------------------
        // 3) 새 토큰 생성 + 저장 (30분 유효)
        String rawToken   = generateToken(48);
        String tokenHash  = new BCryptPasswordEncoder().encode(rawToken);
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);

        u.setPasswordResetTokenHash(tokenHash);
        u.setPasswordResetExpiresAt(expiresAt);
        u.setNeedPasswordReset(true);

        // ------------------------------
        // 4) 메일 발송 (도메인/링크는 환경에 맞게 수정)
        String link = "https://app.example.com/reset?token=" + rawToken;
        String subject = "[서비스명] 비밀번호 재설정 안내";
        String body = """
            아래 링크로 30분 내에 비밀번호를 재설정해주세요.
            %s

            본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
            """.formatted(link);

        mailSender.send(u.getEmail(), subject, body);

        return new PasswordResetIssueResponse("Password reset link sent");
    }


    private String generateToken(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}