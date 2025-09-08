package com.example.hyu.controller;

import com.example.hyu.dto.user.ProfileResponse;
import com.example.hyu.dto.user.ProfileUpdateRequest;
import com.example.hyu.security.CurrentUser;
import com.example.hyu.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProfileResponse> me() {
        Long userId = CurrentUser.id(); // JWT에서 userId를 principal로 세팅했다고 가정
        return ResponseEntity.ok(profileService.getMyProfile(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProfileResponse> upsert(@RequestBody @Valid ProfileUpdateRequest req) {
        Long userId = CurrentUser.id();
        return ResponseEntity.ok(profileService.upsertMyProfile(userId, req));
    }
}
