package com.example.hyu.controller.assessment;

import com.example.hyu.config.TestAuthConfig;
import com.example.hyu.dto.assessment.admin.*;
import com.example.hyu.entity.Assessment;
import com.example.hyu.enums.RiskLevel;
import com.example.hyu.service.assessment.CmsAssessmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CmsAssessmentController 슬라이스(MockMvc) 테스트
 * - 시큐리티 필터 비활성화(addFilters=false)
 * - @WithMockUser(roles="ADMIN") 로 메서드 보안 통과
 * - @Import(TestAuthConfig) 로 @AuthenticationPrincipal(AuthPrincipal) 주입
 */
@WebMvcTest(CmsAssessmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestAuthConfig.class)
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class CmsAssessmentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean CmsAssessmentService service;

    /* =========================
       검사(Assessment)
       ========================= */

    @Test
    @DisplayName("검사 생성 - 201 Created + Location + ID Body")
    void create_assessment_created() throws Exception {
        CmsAssessmentUpsertReq req = new CmsAssessmentUpsertReq(
                "PHQ9", "우울 자가검사", "DEPRESSION",
                "설명입니다", Assessment.Status.ACTIVE
        );
        Mockito.when(service.createAssessment(any(CmsAssessmentUpsertReq.class))).thenReturn(123L);

        mockMvc.perform(post("/api/admin/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/assessments/123"))
                .andExpect(content().string("123"));
    }

    @Test
    @DisplayName("검사 수정 - 204 No Content")
    void update_assessment_noContent() throws Exception {
        CmsAssessmentUpsertReq req = new CmsAssessmentUpsertReq(
                "PHQ9", "우울 자가검사(수정)", "DEPRESSION",
                "수정 설명", Assessment.Status.ARCHIVED
        );
        Mockito.doNothing().when(service).updateAssessment(eq(123L), any(CmsAssessmentUpsertReq.class));

        mockMvc.perform(put("/api/admin/assessments/{id}", 123L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자용 목록(삭제 포함) - 200 OK")
    void adminList_success() throws Exception {
        CmsAssessmentRes a1 = new CmsAssessmentRes(
                1L, "PHQ9", "우울", "DEPRESSION",
                Assessment.Status.ACTIVE, false, null
        );
        CmsAssessmentRes a2 = new CmsAssessmentRes(
                2L, "GAD7", "불안", "ANXIETY",
                Assessment.Status.ARCHIVED, true, Instant.parse("2025-01-01T00:00:00Z")
        );

        Mockito.when(service.adminListIncludingDeleted(any()))
                .thenReturn(new PageImpl<>(List.of(a1, a2), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/admin/assessments/all").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].code").value("PHQ9"))
                .andExpect(jsonPath("$.content[1].deleted").value(true))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("관리자용 단건 조회(삭제 포함) - 200 OK")
    void adminGetOne_success() throws Exception {
        CmsAssessmentRes res = new CmsAssessmentRes(
                10L, "PHQ9", "우울", "DEPRESSION",
                Assessment.Status.ACTIVE, false, null
        );
        Mockito.when(service.adminGetOneIncludingDeleted(10L)).thenReturn(res);

        mockMvc.perform(get("/api/admin/assessments/{id}/any", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.code").value("PHQ9"));
    }

    @Test
    @DisplayName("소프트 삭제 - 204 No Content")
    void softDelete_noContent() throws Exception {
        Mockito.doNothing().when(service).softDelete(12L);

        mockMvc.perform(delete("/api/admin/assessments/{id}", 12L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("복구 - 204 No Content")
    void restore_noContent() throws Exception {
        Mockito.doNothing().when(service).restore(12L);

        mockMvc.perform(post("/api/admin/assessments/{id}/restore", 12L))
                .andExpect(status().isNoContent());
    }

    /* =========================
       문항(Question)
       ========================= */

    @Test
    @DisplayName("문항 전체 교체 - 204 No Content")
    void replaceQuestions_noContent() throws Exception {
        List<CmsQuestionUpsertReq> body = List.of(
                new CmsQuestionUpsertReq(1, "지난 2주 동안 기분이 가라앉았나요?", false),
                new CmsQuestionUpsertReq(2, "수면에 어려움이 있었나요?", false)
        );
        Mockito.doNothing().when(service).replaceQuestions(eq(5L), anyList());

        mockMvc.perform(post("/api/admin/assessments/{id}/questions/replace", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("문항 조회 - 200 OK + 배열 길이")
    void getQuestions_success() throws Exception {
        List<CmsQuestionRes> res = List.of(
                new CmsQuestionRes(1L, 5L, 1, "Q1", false),
                new CmsQuestionRes(2L, 5L, 2, "Q2", true)
        );
        Mockito.when(service.getQuestions(5L)).thenReturn(res);

        mockMvc.perform(get("/api/admin/assessments/{id}/questions", 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].reverseScore").value(true));
    }

    /* =========================
       점수 구간표(Range) - 벌크
       ========================= */

    @Test
    @DisplayName("구간표 전체 교체 - 204 No Content")
    void replaceRanges_noContent() throws Exception {
        List<CmsRangeUpsertReq> body = List.of(
                new CmsRangeUpsertReq(0, 4, RiskLevel.MILD, "약함", "낮습니다", "규칙적인 생활 유지"),
                new CmsRangeUpsertReq(5, 9, RiskLevel.MODERATE, "중간", "중간입니다", "상담을 고려하세요")
        );
        Mockito.doNothing().when(service).replaceRanges(eq(7L), anyList());

        mockMvc.perform(post("/api/admin/assessments/{id}/ranges/replace", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("구간표 조회 - 200 OK")
    void getRanges_success() throws Exception {
        List<CmsRangeRes> res = List.of(
                new CmsRangeRes(1L, 0, 4, RiskLevel.MILD, "약함", "낮습니다", "규칙생활"),
                new CmsRangeRes(2L, 5, 9, RiskLevel.MODERATE, "중간", "중간입니다", "상담 고려")
        );
        Mockito.when(service.getRanges(7L)).thenReturn(res);

        mockMvc.perform(get("/api/admin/assessments/{id}/ranges", 7L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].minScore").value(0))
                .andExpect(jsonPath("$[1].level").value("MODERATE"));
    }

    /* =========================
       점수 구간표(Range) - 단건 CRUD
       ========================= */

    @Test
    @DisplayName("구간 단건 생성 - 201 Created + Location + ID Body")
    void createRange_created() throws Exception {
        CmsRangeUpsertReq body = new CmsRangeUpsertReq(
                10, 14, RiskLevel.RISK, "위험", "높습니다", "전문가 상담 권고"
        );
        Mockito.when(service.createRange(eq(3L), any(CmsRangeUpsertReq.class))).thenReturn(987L);

        mockMvc.perform(post("/api/admin/assessments/{assessmentId}/ranges", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/assessments/ranges/987"))
                .andExpect(content().string("987"));
    }

    @Test
    @DisplayName("구간 단건 조회 - 200 OK")
    void getRange_success() throws Exception {
        CmsRangeRes res = new CmsRangeRes(
                987L, 10, 14, RiskLevel.RISK, "위험", "높습니다", "상담 권고"
        );
        Mockito.when(service.getRange(987L)).thenReturn(res);

        mockMvc.perform(get("/api/admin/assessments/ranges/{rangeId}", 987L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(987))
                .andExpect(jsonPath("$.maxScore").value(14));
    }

    @Test
    @DisplayName("구간 단건 수정 - 204 No Content")
    void updateRange_noContent() throws Exception {
        CmsRangeUpsertReq body = new CmsRangeUpsertReq(
                15, 27, RiskLevel.HIGH_RISK, "초위험", "매우 높습니다", "긴급 상담 권고"
        );
        Mockito.doNothing().when(service).updateRange(eq(555L), any(CmsRangeUpsertReq.class));

        mockMvc.perform(put("/api/admin/assessments/ranges/{rangeId}", 555L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("구간 단건 삭제 - 204 No Content")
    void deleteRange_noContent() throws Exception {
        Mockito.doNothing().when(service).deleteRange(555L);

        mockMvc.perform(delete("/api/admin/assessments/ranges/{rangeId}", 555L))
                .andExpect(status().isNoContent());
    }
}