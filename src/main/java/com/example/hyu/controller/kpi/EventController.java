package com.example.hyu.controller.kpi;

import com.example.hyu.dto.kpi.EventRequest;
import com.example.hyu.service.kpi.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    // 이벤트 수집 api
    // @param request 이벤트 요청 DTO
    // @param idemKey X-Idempotency-Key (헤더, 중복 방지용)
    // @return 저장 결과 (JSON)
    @PostMapping
    public ResponseEntity<Map<String, Object>> ingestEvent(
            @RequestBody EventRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idemKey
    ) {
        Map<String, Object> result = eventService.ingest(request, idemKey);
        return ResponseEntity.ok(result);
    }



}
