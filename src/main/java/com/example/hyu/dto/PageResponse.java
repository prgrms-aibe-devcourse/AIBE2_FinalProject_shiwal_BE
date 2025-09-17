package com.example.hyu.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// 페이지 응답 래퍼
public record PageResponse<T> (
        List<T> content,  // 현재 페이지 데이터
        int page,  // 현재 페이지 번호 (0부터 시작)
        int size,  // 요청한 페이지 크기
        long totalElements,  // 전체 데이터 개수
        int totalPages  // 전체 페이지 수
){
    public static <T> PageResponse<T> from(Page<T> page){
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
