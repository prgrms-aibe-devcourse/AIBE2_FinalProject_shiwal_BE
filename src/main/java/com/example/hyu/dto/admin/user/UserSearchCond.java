package com.example.hyu.dto.admin.user;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UserSearchCond(
        String q,   // 이메일/닉네임/이름 검색
        String role, // USER / ADMIN
        String state,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedTo
) { }