package com.jairomatias.eventix.profile.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.profile.dto.ProfileUpdateForm;
import com.jairomatias.eventix.profile.dto.ProfileUpdateResult;
import com.jairomatias.eventix.profile.service.ProfileService;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String profile(Authentication authentication, Model model) {
        populateModel(authentication.getName(), model);
        return "profile/index";
    }

    @PostMapping
    public String update(
            @Valid @ModelAttribute("profileForm") ProfileUpdateForm form,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "account",
                    profileService.findOwnProfile(authentication.getName()));
            return "profile/index";
        }

        try {
            ProfileUpdateResult result = profileService.updateOwnProfile(
                    authentication.getName(),
                    form);
            if (result.requiresReauthentication()) {
                new SecurityContextLogoutHandler().logout(
                        request,
                        response,
                        authentication);
                return "redirect:/login?profileUpdated";
            }
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Perfil actualizado correctamente.");
            return "redirect:/profile";
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    exception.getField(),
                    "duplicate",
                    exception.getMessage());
            model.addAttribute(
                    "account",
                    profileService.findOwnProfile(authentication.getName()));
            return "profile/index";
        }
    }

    private void populateModel(String authenticatedLogin, Model model) {
        model.addAttribute(
                "account",
                profileService.findOwnProfile(authenticatedLogin));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute(
                    "profileForm",
                    profileService.getOwnUpdateForm(authenticatedLogin));
        }
    }
}
