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

    /**
     * Create a new user account and return its public representation.
     *
     * <p>Validates that the requested email is not already in use, encodes the provided
     * password, assigns the role "USER", persists the user, and returns a UserResponse.</p>
     *
     * @param req request containing signup information (email, password, name, nickname)
     * @return a UserResponse representing the newly created user
     * @throws IllegalArgumentException if the email is already registered
     */
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

    /**
     * Authenticate the user, issue an access token and a refresh token (set as a cookie), persist a login record, and return the access token with its expiry.
     *
     * The method verifies credentials from the provided request, creates a JWT access token, issues a refresh token via the token store and adds it to the HTTP response as an HttpOnly cookie, and saves a UserLogin entry (timestamp and client IP). The returned UserAuthResponse contains the access token and its expires-in seconds.
     *
     * @param req the login request containing at least email and password
     * @param httpReq the incoming HTTP request (used to extract client IP)
     * @param res the HTTP response to which the refresh cookie will be added
     * @return a bearer UserAuthResponse containing the access token and expires-in seconds
     * @throws IllegalArgumentException if the email is not found or the password does not match
     */
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

    /**
     * Refreshes authentication by validating a refresh token, issuing a new access token and rotated refresh token.
     *
     * Validates the refresh token extracted from the request (header or cookie), creates a new access token for the
     * refresh token owner, rotates the refresh token in the token store, and sets the rotated refresh token as a cookie
     * on the response. Returns an access-token response containing the token and its expiry in seconds (falls back to
     * 6 hours if the provider's validity is not configured).
     *
     * @return a UserAuthResponse containing the new access token and its expires-in (seconds)
     * @throws IllegalStateException if the refresh token is missing or invalid
     */
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

    /**
     * Log out the current session by revoking the associated refresh token and expiring the refresh cookie.
     *
     * If a valid refresh token is present in the request (header or cookie) it is revoked; if `allDevices`
     * is true, all refresh tokens for the same owner are revoked, otherwise only the presented token is revoked.
     * The method is idempotent: missing or invalid refresh tokens are ignored. The refresh cookie is always
     * expired on the response.
     *
     * @param req        the incoming HTTP request (checked for refresh token in header or cookies)
     * @param res        the HTTP response to which the expired refresh cookie will be written
     * @param allDevices if true, revoke refresh tokens across all devices for the token owner; if false, revoke only the presented token
     */
    @Transactional
    public void logout(HttpServletRequest req, HttpServletResponse res, boolean allDevices) {
        extractRefreshRaw(req)
                .flatMap(tokenStoreService::checkRefresh)
                .ifPresent(owner -> tokenStoreService.revokeRefresh(owner, allDevices));

        expireRefreshCookie(res); // RT 없어도 항상 제거 시도
    }

    /**
     * Extracts a raw refresh token from the request.
     *
     * Prefers the "X-Refresh-Token" header; if that is missing or blank, looks for a cookie named
     * by jwtProperties.refreshCookieName() (falls back to "RT" when not configured). Returns an
     * empty Optional when no non-blank token is found.
     *
     * @return an Optional containing the refresh token string if present and non-blank, otherwise Optional.empty()
     */

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

    /**
     * Adds a refresh-token cookie to the HTTP response.
     *
     * The cookie value is set to the provided raw refresh token and is emitted via a `Set-Cookie`
     * header with the following attributes:
     * - Path=/ and HttpOnly
     * - Max-Age derived from `jwtProperties.refreshValidityMillis()` or 14 days if not configured
     * - Cookie name from `jwtProperties.refreshCookieName()` or `"RT"` if not configured
     * - `Secure` flag enabled when `jwtProperties.refreshCookieSecure()` is true
     * - `Domain` set when `jwtProperties.refreshCookieDomain()` is configured
     * - `SameSite=None` when `Secure` is true, otherwise `SameSite=Lax`
     *
     * @param refreshRaw the raw refresh token value to store in the cookie
     */
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

    /**
     * Expires the refresh-token cookie on the client by adding a Set-Cookie header that clears the cookie.
     *
     * <p>The cookie name is taken from {@code jwtProperties.refreshCookieName()} or defaults to {@code "RT"}.
     * The header sets: {@code Path=/}, {@code HttpOnly}, {@code Max-Age=0}. If {@code jwtProperties.refreshCookieSecure()}
     * is true the {@code Secure} attribute is added and {@code SameSite=None} is used; otherwise {@code SameSite=Lax}.
     * If {@code jwtProperties.refreshCookieDomain()} is non-null the {@code Domain} attribute is added.</p>
     */
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

    /**
     * Returns the client's IP address, preferring the first entry of the "X-Forwarded-For" header if present and non-blank; otherwise returns {@code HttpServletRequest#getRemoteAddr()}.
     *
     * @return the client IP string (first XFF entry when available, trimmed)
     */
    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
