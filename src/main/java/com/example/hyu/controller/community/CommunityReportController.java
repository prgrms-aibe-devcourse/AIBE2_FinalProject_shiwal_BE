package com.example.hyu.controller.community;

import com.example.hyu.dto.community.ReportCreateRequest;
import com.example.hyu.security.AuthPrincipal;
import com.example.hyu.service.community.CommunityReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityReportController {
    private final CommunityReportService service;

    // 게시글 신고
    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<?> reportPost(@PathVariable("postId") Long postId,
                                        @AuthenticationPrincipal AuthPrincipal me,
                                        @Valid @RequestBody ReportCreateRequest req) {
        Long id = service.reportPost(me.getUserId(), postId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("reportId", id));
    }

    // 댓글 신고
    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<?> reportComment (@PathVariable("commentId") Long commentId,
                                            @AuthenticationPrincipal AuthPrincipal me,
                                            @Valid @RequestBody ReportCreateRequest req){
        Long id = service.reportComment(me.getUserId(), commentId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("reportId", id));
    }

}
