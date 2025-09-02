package com.example.hyu.core.auth;

import com.example.hyu.core.auth.dto.LoginRequest;
import com.example.hyu.core.auth.dto.LoginResponse;
import com.example.hyu.core.auth.dto.SignupRequest;
import com.example.hyu.core.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserSignupService signupService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequest req) {
        Long id = signupService.signup(req);
        return ResponseEntity.ok(id);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        String token = tokenProvider.generate(req.email());
        return ResponseEntity.ok(LoginResponse.of(token, tokenProvider.getExpiresInSeconds()));
    }
}