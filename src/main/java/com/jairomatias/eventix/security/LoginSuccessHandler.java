package com.jairomatias.eventix.security;

import java.io.IOException;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.user.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public LoginSuccessHandler(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        userService.recordSuccessfulLogin(authentication.getName());

        UserPrincipal principal =
                (UserPrincipal) authentication.getPrincipal();

        String destination = principal.isMustChangePassword()
                ? "/auth/change-password?required"
                : "/dashboard";

        response.sendRedirect(
                request.getContextPath() + destination);
    }
}