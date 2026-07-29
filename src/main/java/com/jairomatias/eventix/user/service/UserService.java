package com.jairomatias.eventix.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.user.dto.ChangePasswordForm;
import com.jairomatias.eventix.user.dto.UserCreateForm;
import com.jairomatias.eventix.user.dto.UserDetailsView;
import com.jairomatias.eventix.user.dto.UserListItem;
import com.jairomatias.eventix.user.dto.UserUpdateForm;
import com.jairomatias.eventix.user.entity.UserStatus;

public interface UserService {

    Page<UserListItem> findAll(String term, UserStatus status, Pageable pageable);

    UserDetailsView findById(Long id);

    UserUpdateForm getUpdateForm(Long id);

    Long create(UserCreateForm form);

    void update(Long id, UserUpdateForm form, String authenticatedUsername);

    void activate(Long id);

    void deactivate(Long id, String authenticatedUsername);

    String resetPassword(Long id, String authenticatedUsername);

    void changeOwnPassword(String authenticatedUsername, ChangePasswordForm form);

    void recordSuccessfulLogin(String login);
}
