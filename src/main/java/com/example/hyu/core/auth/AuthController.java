package com.example.hyu.core.auth;

import com.example.hyu.core.auth.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserSignupService signupService;

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@RequestBody @Valid SignupRequest req) {
        Long id = signupService.signup(req);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/ping")  // 점검용
    public String ping() { return "auth-ok"; }
}