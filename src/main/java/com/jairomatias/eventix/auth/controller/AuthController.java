package com.jairomatias.eventix.auth.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.dto.ChangePasswordForm;
import com.jairomatias.eventix.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        boolean authenticated =
                authentication != null
                && authentication.isAuthenticated()
                && !(authentication
                        instanceof AnonymousAuthenticationToken);

        return authenticated
                ? "redirect:/dashboard"
                : "auth/login";
    }

    @GetMapping("/auth/change-password")
    public String changePasswordForm(Model model) {
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute(
                    "changePasswordForm",
                    new ChangePasswordForm());
        }

        return "auth/change-password";
    }

    @PostMapping("/auth/change-password")
    public String changePassword(
            @Valid
            @ModelAttribute("changePasswordForm")
            ChangePasswordForm form,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (!form.getNewPassword().equals(
                form.getConfirmPassword())) {

            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Las contraseñas no coinciden.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/change-password";
        }

        try {
            userService.changeOwnPassword(
                    authentication.getName(),
                    form);
        } catch (BusinessRuleException exception) {
            bindingResult.reject(
                    "password.change",
                    exception.getMessage());

            return "auth/change-password";
        }

        SecurityContextLogoutHandler logoutHandler =
                new SecurityContextLogoutHandler();

        logoutHandler.logout(
                request,
                response,
                authentication);

        return "redirect:/login?passwordChanged";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }
}