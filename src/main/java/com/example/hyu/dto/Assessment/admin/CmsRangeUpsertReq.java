package com.example.hyu.dto.Assessment.admin;

import com.example.hyu.entity.AssessmentSubmission;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * CMS: 점수 구간 업서트(교체) 요청 DTO
 * - level: "MILD" / "MODERATE" / "RISK" / "HIGH_RISK"
 * - [minScore, maxScore] 구간이 서로 겹치지 않도록 서버에서 검증
 */
public record CmsRangeUpsertReq(
        @NotNull @Min(0) Integer minScore,   // 구간 시작(포함)
        @NotNull @Min(0) Integer maxScore,   // 구간 끝(포함)
        AssessmentSubmission.RiskLevel level,       // 위험도 코드
        String labelKo,     // 위험도 라벨 (약함/중간/위험/초위험)
        String summaryKo,   // 요약 문구
        String adviceKo     // 권고 문구 (자세한 설명)
) {}
