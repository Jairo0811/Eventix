package com.jairomatias.eventix.user.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.user.dto.UserCreateForm;
import com.jairomatias.eventix.user.dto.UserListItem;
import com.jairomatias.eventix.user.dto.UserUpdateForm;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/users")
public class UserController {

    private static final int PAGE_SIZE = 10;

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("roles")
    public RoleName[] roles() {
        return RoleName.values();
    }

    @ModelAttribute("statuses")
    public UserStatus[] statuses() {
        return UserStatus.values();
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("lastName", "firstName").ascending());
        Page<UserListItem> users = userService.findAll(term, status, pageable);

        model.addAttribute("users", users);
        model.addAttribute("term", term);
        model.addAttribute("selectedStatus", status);
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new UserCreateForm());
        }
        model.addAttribute("formMode", "create");
        return "users/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("userForm") UserCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "users/form";
        }

        try {
            Long userId = userService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario creado correctamente.");
            return "redirect:/users/" + userId;
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            model.addAttribute("formMode", "create");
            return "users/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "users/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", userService.getUpdateForm(id));
        }
        model.addAttribute("userId", id);
        model.addAttribute("formMode", "edit");
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("userForm") UserUpdateForm form,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            model.addAttribute("formMode", "edit");
            return "users/form";
        }

        try {
            userService.update(id, form, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Usuario actualizado correctamente.");
            return "redirect:/users/" + id;
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("formMode", "edit");
            return "users/form";
        } catch (BusinessRuleException exception) {
            bindingResult.reject("user.update", exception.getMessage());
            model.addAttribute("userId", id);
            model.addAttribute("formMode", "edit");
            return "users/form";
        }
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.activate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Usuario activado correctamente.");
        return "redirect:/users/" + id;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            userService.deactivate(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Usuario desactivado correctamente.");
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/users/" + id;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String temporaryPassword = userService.resetPassword(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Contraseña restablecida correctamente.");
            redirectAttributes.addFlashAttribute("temporaryPassword", temporaryPassword);
        } catch (BusinessRuleException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/users/" + id;
    }
}
