package com.example.hyu.controller.AdminUserPage;

import com.example.hyu.dto.AdminUserPage.ResetConfirmRequest;
import com.example.hyu.service.AdminUserPage.PasswordResetService;
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