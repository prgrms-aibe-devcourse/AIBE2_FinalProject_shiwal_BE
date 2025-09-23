package com.example.hyu.controller.adminUserPage;

import com.example.hyu.config.TestAuthConfig;
import com.example.hyu.dto.adminUserPage.ReportDetailResponse;
import com.example.hyu.dto.adminUserPage.ReportSummaryResponse;
import com.example.hyu.dto.adminUserPage.ReportUpdateRequest;
import com.example.hyu.service.adminUserPage.AdminReportService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false) // 🔸 시큐리티 필터 비활성화(컨트롤러만 테스트)
@Import(TestAuthConfig.class)             // 🔸 @AuthenticationPrincipal(AuthPrincipal) 주입
@ActiveProfiles("test")
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class AdminReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean AdminReportService adminReportService;

    @Test
    @DisplayName("리스트 조회 - 200 OK")
    void list_success() throws Exception {
        // given: record DTO는 생성자로 바로 만듭니다.
        ReportSummaryResponse s1 = new ReportSummaryResponse(
                1L, Instant.parse("2025-01-01T00:00:00Z"),
                101L, "POST", 555L, "SPAM", "PENDING", null
        );
        ReportSummaryResponse s2 = new ReportSummaryResponse(
                2L, Instant.parse("2025-01-02T00:00:00Z"),
                102L, "COMMENT", 666L, "ABUSE", "REVIEWED", Instant.now()
        );

        Mockito.when(adminReportService.list(Mockito.any(), Mockito.any()))
                .thenReturn(new PageImpl<>(List.of(s1, s2), PageRequest.of(0, 20), 2));

        // when & then
        mockMvc.perform(get("/api/admin/reports").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Page 응답의 content 배열 내부 필드 검증 (record 필드명 그대로 사용)
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].reason").value("SPAM"))
                .andExpect(jsonPath("$.content[1].targetType").value("COMMENT"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("단건 조회 - 200 OK")
    void get_success() throws Exception {
        // given
        ReportDetailResponse detail = new ReportDetailResponse(
                10L, Instant.parse("2025-01-03T00:00:00Z"),
                201L, "POST", 777L, "SPAM", "PENDING",
                "상세내용", null, null, null
        );
        Mockito.when(adminReportService.get(10L)).thenReturn(detail);

        // when & then
        mockMvc.perform(get("/api/admin/reports/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.targetType").value("POST"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.description").value("상세내용"));
    }

    @Test
    @DisplayName("상태 업데이트 - 200 OK")
    void update_success() throws Exception {
        // given: 요청 바디(record)
        ReportUpdateRequest req = new ReportUpdateRequest("RESOLVED", "관리자 메모");
        String body = om.writeValueAsString(req);

        // 업데이트 후 응답(record)
        ReportDetailResponse updated = new ReportDetailResponse(
                10L, Instant.now(),
                201L, "POST", 777L, "SPAM", "RESOLVED",
                "처리됨", null, Instant.now(), "관리자 메모"
        );
        Mockito.when(adminReportService.update(Mockito.eq(10L), Mockito.any(), Mockito.anyLong()))
                .thenReturn(updated);

        // when & then
        mockMvc.perform(patch("/api/admin/reports/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminNote").value("관리자 메모"));
    }
}