package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "assessments",
        uniqueConstraints = @UniqueConstraint(name = "uq_assessment_code", columnNames = "code"),
        indexes = {
                @Index(name = "ix_assessment_status", columnList = "status"),
                @Index(name = "ix_assessment_deleted_at", columnList = "is_deleted, deleted_at")
        }
)
@SQLDelete(sql = "UPDATE assessments SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP, status = 'ARCHIVED' WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Assessment extends BaseTimeEntity {

    public enum Status { ACTIVE, ARCHIVED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=50)
    private String code;

    @Column(nullable=false, length=100)
    private String name;

    @Column(nullable=false, length=50)
    private String category;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy="assessment", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("orderNo ASC")
    @Builder.Default
    private List<AssessmentQuestion> questions = new ArrayList<>();

    @Column(name="is_deleted", nullable=false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name="deleted_at")
    private Instant deletedAt;

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        // 복구 시 상태는 비즈니스에 맞게: 보통 ARCHIVED 유지하거나 ACTIVE로 바꾸는 로직을 서비스에서 결정
    }
}