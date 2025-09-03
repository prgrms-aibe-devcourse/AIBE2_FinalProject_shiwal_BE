package com.example.hyu.service;

import com.example.hyu.dto.user.UserAuthResponse;
import com.example.hyu.dto.user.UserLoginRequest;
import com.example.hyu.dto.user.UserResponse;
import com.example.hyu.dto.user.UserSignupRequest;
import com.example.hyu.dto.user.UserMapper;
import com.example.hyu.entity.User;
import com.example.hyu.entity.UserLogin;
import com.example.hyu.repository.UserLoginRepository;
import com.example.hyu.repository.UserRepository;
import com.example.hyu.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserLoginRepository userLoginRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user account.
     *
     * Creates and persists a User with the provided signup information and returns a UserResponse.
     *
     * @param req the signup request containing email, password, name, and nickname
     * @return a response DTO representing the newly created user
     * @throws IllegalArgumentException if an account with the given email already exists
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
     * Authenticates a user, issues a JWT bearer token, and records the login event.
     *
     * <p>Verifies the provided credentials; on success it generates a JWT for the user,
     * persists a UserLogin record (timestamp and client IP), and returns a bearer token
     * response that includes the token and its remaining validity in seconds (1 hour).
     *
     * @param req the login request containing the user's email and password
     * @param httpReq the incoming HTTP request used to extract the client IP address
     * @return a UserAuthResponse containing the bearer token and its validity (in seconds)
     * @throws IllegalArgumentException if the email is not found or the password is incorrect
     */
    @Transactional
    public UserAuthResponse login(UserLoginRequest req, HttpServletRequest httpReq) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 토큰 발급
        long validityMillis = 1000L * 60 * 60; // 1시간
        String token = jwtTokenProvider.createToken(user.getId(), user.getRole());

        // 로그인 기록 저장
        UserLogin loginLog = UserLogin.builder()
                .loggedAt(Instant.now())
                .ip(extractClientIp(httpReq))
                .field(null)
                .user(user)   // @ManyToOne 매핑한 경우
                .build();

        userLoginRepository.save(loginLog);

        return UserAuthResponse.bearer(token, validityMillis / 1000); // 초 단위 만료 시간
    }

    /**
     * Determine the client's IP address from the request.
     *
     * Prefers the first IP listed in the "X-Forwarded-For" header (comma-separated) when present and non-blank;
     * otherwise falls back to the request's remote address.
     *
     * @return the client's IP address as a String
     */
    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
