package com.example.hyu.service;

import com.example.hyu.dto.user.UserAuthResponse;
import com.example.hyu.dto.user.UserLoginRequest;
import com.example.hyu.dto.user.UserMapper;
import com.example.hyu.dto.user.UserResponse;
import com.example.hyu.dto.user.UserSignupRequest;
import com.example.hyu.entity.User;
import com.example.hyu.entity.UserLogin;
import com.example.hyu.repository.UserLoginRepository;
import com.example.hyu.repository.UserRepository;
import com.example.hyu.security.JwtProperties;
import com.example.hyu.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStoreService tokenStoreService;
    private final JwtProperties jwtProperties;

    /** 회원가입 */
    @Transactional
    public UserResponse signup(UserSignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .name(req.name())
                .nickname(req.nickname())
                .role("USER")
                .build();

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    /** 로그인 + Access 발급 + Refresh 발급(쿠키) + 로그인기록 저장 */
    @Transactional
    public UserAuthResponse login(UserLoginRequest req, HttpServletRequest httpReq, HttpServletResponse res) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String access = jwtTokenProvider.createToken(user.getId(), user.getRole(), user.getEmail());
        String refresh = tokenStoreService.issueRefresh(user.getId(), user.getRole(), user.getEmail());
        addRefreshCookie(res, refresh);

        // 로그인 이력
        UserLogin loginLog = UserLogin.builder()
                .loggedAt(Instant.now())
                .ip(extractClientIp(httpReq))
                .field(null)
                .user(user)
                .build();
        userLoginRepository.save(loginLog);

        Long validityMs = jwtTokenProvider.getValidityMillis(); // null일 수 있음
        long expiresInSec = Math.floorDiv(validityMs != null ? validityMs : 21600000L, 1000); // 기본 6h
        return UserAuthResponse.bearer(access, expiresInSec);
    }

    /** 리프레시(재발급+회전) */
    @Transactional
    public UserAuthResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String raw = extractRefreshRaw(req)
                .orElseThrow(() -> new IllegalStateException("리프레시 토큰이 없습니다."));

        var owner = tokenStoreService.checkRefresh(raw)
                .orElseThrow(() -> new IllegalStateException("리프레시 토큰이 유효하지 않습니다."));

        String newAccess = jwtTokenProvider.createToken(owner.userId(), owner.role(), owner.email());
        String newRefresh = tokenStoreService.rotateRefresh(owner);
        addRefreshCookie(res, newRefresh);

        Long validityMs = jwtTokenProvider.getValidityMillis(); // null일 수 있음
        long expiresInSec = Math.floorDiv(validityMs != null ? validityMs : 21600000L, 1000); // 기본 6h
        return UserAuthResponse.bearer(newAccess, expiresInSec);
    }

    /** 로그아웃(멱등) */
    @Transactional
    public void logout(HttpServletRequest req, HttpServletResponse res, boolean allDevices) {
        extractRefreshRaw(req)
                .flatMap(tokenStoreService::checkRefresh)
                .ifPresent(owner -> tokenStoreService.revokeRefresh(owner, allDevices));

        expireRefreshCookie(res); // RT 없어도 항상 제거 시도
    }

    // ===== 내부 유틸 (별도 클래스 불필요) =====

    private Optional<String> extractRefreshRaw(HttpServletRequest req) {
        // 1) 헤더 우선 (원하면 프론트 디버깅에 유용)
        String h = req.getHeader("X-Refresh-Token");
        if (h != null && !h.isBlank()) return Optional.of(h.trim());

        // 2) 쿠키
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return Optional.empty();
        String cookieName = jwtProperties.refreshCookieName() != null ? jwtProperties.refreshCookieName() : "RT";
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank());
    }

    private void addRefreshCookie(HttpServletResponse res, String refreshRaw) {
        long ttlMs = jwtProperties.refreshValidityMillis() != null
                ? jwtProperties.refreshValidityMillis()
                : 14L * 24 * 3600 * 1000;
        int maxAge = (int) Duration.ofMillis(ttlMs).toSeconds();

        String name = jwtProperties.refreshCookieName() != null ? jwtProperties.refreshCookieName() : "RT";
        boolean secure = jwtProperties.refreshCookieSecure() != null && jwtProperties.refreshCookieSecure();
        String domain = jwtProperties.refreshCookieDomain();

        // SameSite 제어를 위해 Set-Cookie로 직접 추가
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(refreshRaw)
                .append("; Path=/; HttpOnly; Max-Age=").append(maxAge).append("; ");
        if (secure) sb.append("Secure; ");
        if (domain != null) sb.append("Domain=").append(domain).append("; ");
        sb.append("SameSite=").append(secure ? "None" : "Lax");
        res.addHeader("Set-Cookie", sb.toString());
    }

    private void expireRefreshCookie(HttpServletResponse res) {
        String name = jwtProperties.refreshCookieName() != null ? jwtProperties.refreshCookieName() : "RT";
        boolean secure = jwtProperties.refreshCookieSecure() != null && jwtProperties.refreshCookieSecure();
        String domain = jwtProperties.refreshCookieDomain();

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=; Path=/; HttpOnly; Max-Age=0; ");
        if (secure) sb.append("Secure; ");
        if (domain != null) sb.append("Domain=").append(domain).append("; ");
        sb.append("SameSite=").append(secure ? "None" : "Lax");
        res.addHeader("Set-Cookie", sb.toString());
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
