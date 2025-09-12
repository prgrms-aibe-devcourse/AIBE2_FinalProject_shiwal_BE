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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.hyu.security.AuthPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal AuthPrincipal me) {
        return ResponseEntity.ok(profileService.getMyProfile(me.getUserId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProfileResponse> upsert(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestBody @Valid ProfileUpdateRequest req
    ) {
        return ResponseEntity.ok(profileService.upsertMyProfile(me.getUserId(), req));
    }
}
