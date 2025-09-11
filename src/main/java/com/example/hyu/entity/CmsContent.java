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
                "SET `deleted` = true, `deletedAt` = CURRENT_TIMESTAMP, `deletedBy` = NULL " +  // 필요시 삭제자는 서비스에서 세팅
                "WHERE `id` = ?")
@Where(clause = "deleted = false")
public class CmsContent extends BaseTimeEntity{

    public enum Category { MUSIC, MEDITATION, MOOD_BOOST }
    public enum MediaType { AUDIO, VIDEO, TEXT, LINK }
    public enum Visibility { PUBLIC, PRIVATE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //콘텐츠 ID

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private Category category;  //카테고리

    //오른쪽 묶음 구분용 (예: 기분별, 자연의소리, 호흡가이드)
    @Column(length = 50, nullable = false)
    private String groupKey;

    //UI 정렬용
    private Integer displayOrder;  //순서

    @Column(length = 200)
    private String title; //제목

    @Column(columnDefinition = "TEXT")
    private String text; // 문구(확언/유머 등)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MediaType mediaType; //미디어타입

    private Integer duration; //길이(초)

    @Column(length = 500)
    private String thumbnailUrl; //썸네일

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Visibility visibility; //공개범위

    private Instant publishedAt;  //공개시각

    @Column(nullable = false)
    private Long createdBy; //작성자

    @Column(nullable = false)
    private Long updatedBy;  //수정자

    // ===== 소프트 삭제 필드 =====
    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false; //삭제 여부

    private Instant deletedAt; //삭제 시각

    private Long deletedBy; //삭제자

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