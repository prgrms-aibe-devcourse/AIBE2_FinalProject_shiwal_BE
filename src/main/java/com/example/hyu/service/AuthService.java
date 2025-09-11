package com.example.hyu.service;

import com.example.hyu.dto.user.UserAuthResponse;
import com.example.hyu.dto.user.UserLoginRequest;
import com.example.hyu.dto.user.UserMapper;
import com.example.hyu.dto.user.UserResponse;
import com.example.hyu.dto.user.UserSignupRequest;
import com.example.hyu.entity.Users;
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

    /** 회원가입 */
    @Transactional
    public UserResponse signup(UserSignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        Users user = Users.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .name(req.name())
                .nickname(req.nickname())
                .role("USER")
                .build();

        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    /** 로그인 + JWT 발급 + 로그인기록 저장 */
    @Transactional
    public UserAuthResponse login(UserLoginRequest req, HttpServletRequest httpReq) {
        Users user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        //계정 상태 체크(정지/탈퇴 차단 + 자동해제)
        Instant now = Instant.now();

        //정지 만료 자동 해제
        if(user.getState() == Users.UserState.SUSPENDED){
            if(user.getSuspendUntil() != null && user.getSuspendUntil().isBefore(now)){
                user.setState(Users.UserState.ACTIVE);
                user.setSuspendUntil(null);
            }else {
                //아직 정지 중
                String untilTxt = user.getSuspendUntil() != null ? user.getSuspendUntil().toString() : "until manual unlock";
            }
        }

        //탈퇴 차단
        if(user.getState() == Users.UserState.WITHDRAWN){
            throw new IllegalStateException("ACCOUNT_WITHDRAWN");
        }

        // 이메일 포함하여 토큰 발급 (팀원 요구사항)
        String token = jwtTokenProvider.createToken(user.getId(), user.getRole(), user.getEmail());

        // 로그인 이력 적재 (ManyToOne 매핑 기준)
        UserLogin loginLog = UserLogin.builder()
                .loggedAt(Instant.now())
                .ip(extractClientIp(httpReq))
                .field(null)
                .user(user)
                .build();
        userLoginRepository.save(loginLog);

        long expiresInSec = jwtTokenProvider.getValidityMillis() / 1000;
        return UserAuthResponse.bearer(token, expiresInSec);
    }

    private String extractClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
