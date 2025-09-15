package com.example.hyu.dto.kpi;

import java.util.Map;

public record EventRequest (
        Long userId, // 이벤트를 발생시킨 유저id
        String eventName, // 어떤 이벤트인가 (종류)
        String eventTime, // 이벤트 발생 시각
        String status, // 성공/실패
        String level, // 위험 수준(
        String sessionId, //같은 대화 or 검사 묶음 구분
        Map<String, Object> meta // 기타 부가 정보(자유롭게)
){}
