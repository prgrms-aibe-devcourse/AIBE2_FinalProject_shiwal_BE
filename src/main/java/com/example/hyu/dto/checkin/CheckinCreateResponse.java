package com.example.hyu.dto.checkin;

import java.time.LocalDate;

public record CheckinCreateResponse(
        boolean created,  // 새로 생성했는지(이미 있으면 false)
        LocalDate date,
        int streak
) {}
