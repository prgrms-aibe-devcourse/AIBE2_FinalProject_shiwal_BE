package com.example.hyu.dto.kpi;
// =====================
// 리텐션 단일 결과 행(D1/D7/D30)
// =====================

import java.time.LocalDate;
import java.math.BigDecimal;

public record RetentionRow(
        LocalDate cohortDay,         // <가입 기준일> (예: 2025-09-01 가입자 그룹)
        String window,               // <리텐션 구간> "D1" | "D7" | "D30"
        long usersTotal,             // <해당일 신규 가입자 수>
        long usersReturned,          // <리텐션 성공자 수> window 구간에 다시 돌아온 사용자 수
        BigDecimal rate              // <리텐션 비율> returned / total * 100 (소수점 둘째 자리 반올림)
) {}