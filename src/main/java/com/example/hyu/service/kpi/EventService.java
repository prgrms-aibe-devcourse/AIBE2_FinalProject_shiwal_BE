package com.example.hyu.service.kpi;

import com.example.hyu.dto.kpi.EventRequest;
import com.example.hyu.entity.Event;
import com.example.hyu.repository.kpi.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 이벤트 저장 (멱등 키 지원)
    // @param request EventRepository DTO
    // @param idemKey X-Idenmpotency-Key (중복 방지용)
    // @return 저장 결과 (Map)
    public Map<String, Object> ingest(EventRequest request, String idemKey){
        // 위험 이벤트면 level 필수 체크
        if("risk_detected".equals(request.eventName())){
            if(request.level() == null || request.level().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "level required for ." +
                        "risk_detected");
            }
        }

        // 멱등 처리 ( 이미 같은 키로 저장된 이벤트가 있는지 확인)
        if (idemKey != null && !idemKey.isBlank()) {
            var existing = eventRepository.findByIdempotencyKey(idemKey);
            if(existing.isPresent()) {
                return Map.of(
                        "ok", true,
                        "id", existing.get().getId(),
                        "dedup", true
                );
            }
        }

        // eventTime 파싱 (ISO-8601 UTC 형식 -> LocalDateTime
        LocalDateTime eventTimeUtc;
        try {
            Instant instant = Instant.parse(request.eventTime());
            eventTimeUtc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid eventTime (use ISO-8601 UTC");
        }

        // 이벤트 엔티티 생성
        Event event = new Event();
        event.setUserId(request.userId());
        event.setEventName(request.eventName());
        event.setEventTime(eventTimeUtc);
        event.setStatus((request.status() == null || request.status().isBlank()) ? "ok" : request.status());
        event.setLevel(request.level());
        event.setSessionId(request.sessionId());
        event.setIdempotencyKey(idemKey);

        try {
            event.setMeta(
                    request.meta() == null ? "{}" : objectMapper.writeValueAsString(request.meta())
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meta must be JSON-serializable");
        }

        // 저장
        eventRepository.save(event);

        return Map.of(
                "ok", true,
                "id", event.getId()
        );
    }

}
