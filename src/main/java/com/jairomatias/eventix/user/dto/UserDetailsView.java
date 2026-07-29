package com.jairomatias.eventix.user.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.UserStatus;

public record UserDetailsView(
        Long id,
        String firstName,
        String lastName,
        String email,
        String username,
        String phone,
        RoleName roleName,
        UserStatus status,
        boolean mustChangePassword,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt,
        String createdBy,
        String updatedBy) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
