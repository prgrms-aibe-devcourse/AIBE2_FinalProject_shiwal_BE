package com.example.hyu.controller;

import com.example.hyu.dto.admin.user.ResetConfirmRequest;
import com.example.hyu.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/password-reset")
@RequiredArgsConstructor
public class PasswordResetConfirmController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody ResetConfirmRequest req) {
        passwordResetService.confirm(req);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }
}