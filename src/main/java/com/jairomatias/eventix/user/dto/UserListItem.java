package com.jairomatias.eventix.user.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.UserStatus;

public record UserListItem(
        Long id,
        String firstName,
        String lastName,
        String email,
        String username,
        RoleName roleName,
        UserStatus status,
        LocalDateTime lastLoginAt) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
