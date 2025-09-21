package com.example.hyu.dto;

import org.springframework.data.domain.Slice;
import java.util.List;

/** 무한 스크롤 응답 래퍼 (총 개수 없음) */
public record SliceResponse<T>(
        List<T> content, // 현재 페이지 데이터
        int page,        // 현재 페이지 번호 (0부터 시작)
        int size, // 요청한 페이지 크기
        boolean hasNext  // 다음 페이지 존재 여부
) {
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}