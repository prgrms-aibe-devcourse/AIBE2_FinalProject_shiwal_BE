package com.example.hyu.controller.adminUserPage;

import com.example.hyu.config.TestAuthConfig;
import com.example.hyu.dto.adminUserPage.ChangeStateRequest;
import com.example.hyu.dto.adminUserPage.UserSearchCond;
import com.example.hyu.dto.adminUserPage.UserSummaryResponse;
import com.example.hyu.service.adminUserPage.AdminUserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)   // 시큐리티 필터는 비활성화
@Import(TestAuthConfig.class)               // @AuthenticationPrincipal(AuthPrincipal) 주입 보조
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")              // 메서드 보안(@PreAuthorize) 통과용
class AdminUserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean AdminUserService adminUserService;

    @Test
    @DisplayName("사용자 목록 조회 - 200 OK")
    void list_success() throws Exception {
        // given: record 생성자는 필드 순서대로
        UserSummaryResponse u1 = new UserSummaryResponse(
                1L, "김철수", "kim", "kim@example.com",
                "USER", "ACTIVE", "MILD",
                Instant.parse("2025-01-01T00:00:00Z")
        );
        UserSummaryResponse u2 = new UserSummaryResponse(
                2L, "관리자", "admin", "admin@example.com",
                "ADMIN", "ACTIVE", "RISK",
                Instant.parse("2025-01-02T00:00:00Z")
        );

        Mockito.when(adminUserService.list(any(UserSearchCond.class), any()))
                .thenReturn(new PageImpl<>(List.of(u1, u2), PageRequest.of(0, 20), 2));

        // when & then
        mockMvc.perform(get("/api/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nickname").value("kim"))
                .andExpect(jsonPath("$.content[0].role").value("USER"))
                .andExpect(jsonPath("$.content[1].role").value("ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("사용자 상태 변경 - 200 OK")
    void changeState_success() throws Exception {
        // given: 요청 record
        ChangeStateRequest req = new ChangeStateRequest(
                "SUSPENDED",                // state
                "욕설 다수",                 // reason (선택)
                "P2W",                      // period (ISO-8601 기간)
                "MODERATE"                  // riskLevel
        );
        String body = om.writeValueAsString(req);

        // 변경 이후 응답 record
        UserSummaryResponse changed = new UserSummaryResponse(
                1L, "김철수", "kim", "kim@example.com",
                "USER", "SUSPENDED", "MODERATE",
                Instant.parse("2025-01-01T00:00:00Z")
        );

        Mockito.when(adminUserService.changeState(eq(1L), any(ChangeStateRequest.class)))
                .thenReturn(changed);

        // when & then
        mockMvc.perform(patch("/api/admin/users/{id}/state", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.state").value("SUSPENDED"))
                .andExpect(jsonPath("$.riskLevel").value("MODERATE"));
    }
}