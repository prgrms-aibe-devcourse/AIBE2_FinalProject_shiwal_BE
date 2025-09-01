package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) @Builder
@Entity @Table(name = "cms_contents")
public class CmsContent {

    public enum Category { MUSIC, MEDITATION, MOOD_BOOST }
    public enum MediaType { AUDIO, VIDEO, TEXT, LINK }
    public enum Visibility { PUBLIC, PRIVATE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "콘텐츠 ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "카테고리", length = 30, nullable = false)
    private Category category;

    @Column(name = "제목", length = 200)
    private String title;

    @Lob
    @Column(name = "문구")
    private String text; // 확언/유머 등

    @Enumerated(EnumType.STRING)
    @Column(name = "미디어 타입", length = 20)
    private MediaType mediaType;

    @Column(name = "길이(초)")
    private Integer duration;

    @Column(name = "썸네일", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "공개범위", length = 10)
    private Visibility visibility;

    @Column(name = "공개 시각")
    private Instant publishedAt;

    @Column(name = "생성", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "수정")
    private Instant updatedAt;

    @Column(name = "작성자", nullable = false)
    private Long createdBy;

    @Column(name = "수정자", nullable = false)
    private Long updatedBy;
}