package com.example.hyu.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table( //중복 응답( 같은 submission_id + 같은 question_id)을 차단
        name = "assessment_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_answer_submission_question",
                columnNames = {"submission_id", "question_id"}
        ),
        indexes = {
                @Index(name="idx_answer_submission", columnList="submission_id"),
                @Index(name="idx_answer_question",   columnList="question_id")
        }
)
@Getter
@Setter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Check(constraints = "selected_value BETWEEN 0 AND 3") //점수값 범위(0~3) 보장
public class AssessmentAnswer extends BaseTimeEntity { //사용자가 각 문항에서 선택한 문항을 나타내는 엔티티

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;  // PK, 응답 고유번호

    @Column(nullable=false)
    @jakarta.validation.constraints.Min(0) //최솟값 0
    @jakarta.validation.constraints.Max(3) //최댓값 3
    private Integer selectedValue;
    // 사용자가 선택한 값 (0~3)

    @Column(length=100)
    private String rawAnswer;
    // 사람이 읽는 선택지 라벨(예: 전혀 아니다) 등 저장하고 싶으면 사용, 필수 아님

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="submission_id", nullable=false)
    private AssessmentSubmission submission;
    // 이 응답이 속한 검사 제출(Submission)

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="question_id", nullable=false)
    private AssessmentQuestion question;
    // 어떤 문항에 대한 응답인지


    //문항별 응답을 바꿀 때 쓰는 메서드
    public void revise(int value, String raw){
        //값 검증
        if(value < 0 || value > 3){
            throw new IllegalArgumentException("selectedValue must be 0~3");
        }
        this.selectedValue = value;
        this.rawAnswer = raw;
    }

    public void attachTo(AssessmentSubmission submission){
        this.submission = submission;
        submission.getAnswers().add(this);
    }



}