package com.jairomatias.eventix.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.auth.dto.ForgotPasswordForm;
import com.jairomatias.eventix.auth.dto.ResetPasswordForm;
import com.jairomatias.eventix.auth.service.PasswordRecoveryService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.dto.ChangePasswordForm;
import com.jairomatias.eventix.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(UserService userService) {
        this.userService = userService;
        this.passwordRecoveryService = null;
    }

    @Autowired
    public AuthController(
            UserService userService,
            PasswordRecoveryService passwordRecoveryService) {
        this.userService = userService;
        this.passwordRecoveryService = passwordRecoveryService;
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

    @GetMapping("/login/forgot-password")
    public String forgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/login/forgot-password")
    public String forgotPassword(
            @Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        passwordRecoveryService.requestReset(form.getEmail());
        redirectAttributes.addFlashAttribute("recoveryRequested", true);
        return "redirect:/login/forgot-password";
    }

    @GetMapping("/login/reset-password")
    public String resetPasswordForm(
            @RequestParam(required = false) String token,
            Model model) {
        if (!passwordRecoveryService.isTokenValid(token)) {
            model.addAttribute("invalidToken", true);
            return "auth/reset-password";
        }

        if (!model.containsAttribute("resetPasswordForm")) {
            ResetPasswordForm form = new ResetPasswordForm();
            form.setToken(token);
            model.addAttribute("resetPasswordForm", form);
        }
        return "auth/reset-password";
    }

    @PostMapping("/login/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
            BindingResult bindingResult) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Las contraseñas no coinciden.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            passwordRecoveryService.resetPassword(
                    form.getToken(),
                    form.getNewPassword(),
                    form.getConfirmPassword());
        } catch (BusinessRuleException exception) {
            bindingResult.reject("password.reset", exception.getMessage());
            return "auth/reset-password";
        }

        return "redirect:/login?passwordChanged";
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
