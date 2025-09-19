package com.example.hyu.dto.user;

import com.example.hyu.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getNickname(),
                u.getRole()
        );
    }
}
