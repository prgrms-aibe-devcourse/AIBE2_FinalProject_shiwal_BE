package com.example.hyu.dto.checkin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CheckinCreateRequest(
        Instant clientAt,                 // 선택: 클라이언트 타임스탬프
        String tz,                        // 선택: "Asia/Seoul"
        String source,                    // 선택: WEB/APP/SCHEDULED
        String type,                      // 선택: MANUAL/AUTO
        @Min(1) @Max(5) Integer mood,     // 선택(권장): 1~5
        @Size(max = 200) String note,     // 선택
        @Min(1) @Max(5) Integer energy,   // 선택
        @Min(1) @Max(5) Integer stress,   // 선택
        Double sleepHours,                // 선택: 0.0~24.0 (검증은 서비스에서)
        List<String> tags,                // 선택
        Boolean privacy,                  // 선택
        Map<String, Object> meta          // 선택
) {}