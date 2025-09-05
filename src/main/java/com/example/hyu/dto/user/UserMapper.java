package com.example.hyu.dto.user;

import com.example.hyu.entity.Users;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(Users u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getNickname(),
                u.getRole()
        );
    }
}
