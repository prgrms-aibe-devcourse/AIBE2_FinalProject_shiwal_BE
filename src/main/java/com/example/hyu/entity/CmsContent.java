package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Table(name = "cms_contents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLDelete(sql =
        "UPDATE cms_contents " +
                "SET `삭제여부` = true, `삭제시각` = CURRENT_TIMESTAMP, `삭제자` = NULL " +  // 필요시 삭제자는 서비스에서 세팅
                "WHERE `콘텐츠 ID` = ?")
@Where(clause = "`삭제여부` = false")
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

    // 👉 오른쪽 묶음 구분용 (예: 기분별, 자연의소리, 호흡가이드)
    @Column(name = "그룹", length = 50, nullable = false)
    private String groupKey;

    // 👉 UI 정렬용
    @Column(name = "순서")
    private Integer displayOrder;

    @Column(name = "제목", length = 200)
    private String title;

    @Column(name = "문구", columnDefinition = "TEXT")
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

    // ===== 소프트 삭제 필드 =====
    @Builder.Default
    @Column(name = "삭제여부", nullable = false)
    private boolean deleted = false;

    @Column(name = "삭제시각")
    private Instant deletedAt;

    @Column(name = "삭제자")
    private Long deletedBy;

    // ===== 소프트 삭제/복구 편의 메서드 =====
    public void markDeleted(Long adminId) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = adminId;
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}