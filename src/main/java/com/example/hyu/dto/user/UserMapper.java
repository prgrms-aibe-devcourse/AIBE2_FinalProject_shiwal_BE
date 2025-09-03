package com.example.hyu.dto.user;

import com.example.hyu.entity.User;

public final class UserMapper {

    /**
 * Private constructor to prevent instantiation of this utility class.
 *
 * UserMapper contains only static mapping methods and should not be instantiated.
 */
private UserMapper() {}

    /**
     * Maps a User entity to a UserResponse DTO.
     *
     * @param u the source User; must not be null
     * @return a new UserResponse containing the id, email, name, nickname, and role from {@code u}
     */
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
