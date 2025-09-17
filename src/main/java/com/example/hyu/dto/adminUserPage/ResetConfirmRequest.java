package com.example.hyu.dto.adminUserPage;


//사용자 비밀번호 재설정 확정 요청
public record ResetConfirmRequest(
        String token,
        String newPassword
) { }
