package com.example.hyu.controller;

import com.example.hyu.dto.user.UserAuthResponse;
import com.example.hyu.dto.user.UserLoginRequest;
import com.example.hyu.dto.user.UserResponse;
import com.example.hyu.dto.user.UserSignupRequest;
import com.example.hyu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody @Valid UserSignupRequest req) {
        return ResponseEntity.ok(authService.signup(req));
    }

    // ★ Response로 교체: 로그인 시 RT 쿠키 내려주기 위함
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody @Valid UserLoginRequest req,
                                                  HttpServletResponse res,
                                                  HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.login(req, httpReq, res));
    }

    // ★ 리프레시(재발급+회전)
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        return ResponseEntity.ok(authService.refresh(req, res));
    }

    // ★ 로그아웃(멱등: RT 없어도 204)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam(defaultValue = "false") boolean allDevices,
                                       HttpServletRequest req, HttpServletResponse res) {
        authService.logout(req, res, allDevices);
        return ResponseEntity.noContent().build();
    }
}
