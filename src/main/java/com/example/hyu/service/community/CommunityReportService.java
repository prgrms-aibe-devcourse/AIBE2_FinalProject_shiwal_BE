package com.example.hyu.service.community;

import com.example.hyu.dto.community.ReportCreateRequest;
import com.example.hyu.entity.Report;
import com.example.hyu.repository.ReportRepository;
import com.example.hyu.repository.community.CommunityCommentRepository;
import com.example.hyu.repository.community.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityReportService {

    private final ReportRepository reportRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;

    // 중복 신고 방지 : 동일 유저가 동일 대상에 대해 24시간 내 다시 신고 불가
    private static final int DEDUP_HOURS = 24;

    // 게시글 신고
    public Long reportPost(Long reporterId, Long postId, ReportCreateRequest req) {
        // 대상 존재 여부 확인
        postRepository.findById(postId)
                .orElseThrow(()-> new RuntimeException("POST_NOT_FOUND"));

        // 최근 신고 여부 확인
        Instant since = Instant.now().minus(DEDUP_HOURS, ChronoUnit.HOURS);
        reportRepository.findTop1ByReporterIdAndTargetTypeAndTargetIdAndReportedAtAfterOrderByReportedAtDesc(
                reporterId, Report.TargetType.POST, postId, since
        )
                .ifPresent(r -> {
                    throw new RuntimeException("DUPLICATE_REPORT_RECENTLY");
                });

        // 신고 저장
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(Report.TargetType.POST)
                .targetId(postId)
                .reason(Report.Reason.valueOf(req.reason().name()))
                .description(req.description())
                .attachmentUrl(req.attachmentUrl())
                .reportedAt(Instant.now())
                .status(Report.Status.PENDING)
                .build();

        return reportRepository.save(report).getId();
    }

    // 댓글 신고
    public Long reportComment(Long reporterId, Long commentId, ReportCreateRequest req) {
        // 대상 존재 여부 확인
        commentRepository.findById(commentId)
                .orElseThrow(()-> new RuntimeException("COMMENT_NOT_FOUND"));

        // 최근 신고 여부 확인
        Instant since = Instant.now().minus(DEDUP_HOURS, ChronoUnit.HOURS);
        reportRepository.findTop1ByReporterIdAndTargetTypeAndTargetIdAndReportedAtAfterOrderByReportedAtDesc(
                reporterId, Report.TargetType.COMMENT, commentId, since
        )
                .ifPresent(r -> {
                    throw new RuntimeException("DUPLICATE_REPORT_RECENTLY");
                });

        // 신고 저장
        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(Report.TargetType.COMMENT)
                .targetId(commentId)
                .reason(Report.Reason.valueOf(req.reason().name()))
                .description(req.description())
                .attachmentUrl(req.attachmentUrl())
                .reportedAt(Instant.now())
                .status(Report.Status.PENDING)
                .build();

        return reportRepository.save(report).getId();
    }
























}
