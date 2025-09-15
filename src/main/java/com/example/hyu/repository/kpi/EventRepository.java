package com.example.hyu.repository.kpi;

import com.example.hyu.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    //멱등 키 (idempotencyKey)로 중복 확인할 때 사용
    Optional<Event> findByIdempotencyKey(String idempotencyKey);
}
