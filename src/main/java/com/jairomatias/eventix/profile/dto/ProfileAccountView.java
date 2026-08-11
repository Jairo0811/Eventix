package com.jairomatias.eventix.profile.dto;

import java.time.LocalDateTime;

import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.UserStatus;

public record ProfileAccountView(
        String fullName,
        String username,
        String email,
        String phone,
        RoleName role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt) {
}
