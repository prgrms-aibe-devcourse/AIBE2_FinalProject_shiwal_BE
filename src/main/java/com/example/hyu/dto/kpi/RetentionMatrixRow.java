package com.example.hyu.dto.kpi;
// =====================
// 리텐션 매트릭스 (코호트별)
// =====================

import java.time.LocalDate;
import java.math.BigDecimal;

public record RetentionMatrixRow(
        LocalDate cohortDay,         // <가입 기준일>
        long usersTotal,             // <해당일 신규 가입자 수>
        long d1Returned,             // <D1 복귀 수>
        BigDecimal d1Rate,           // <D1 리텐션율>
        long d7Returned,             // <D7 복귀 수>
        BigDecimal d7Rate,           // <D7 리텐션율>
        long d30Returned,            // <D30 복귀 수>
        BigDecimal d30Rate           // <D30 리텐션율>
) {}
