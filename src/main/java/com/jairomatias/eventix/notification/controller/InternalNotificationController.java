package com.jairomatias.eventix.notification.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jairomatias.eventix.notification.service.InternalNotificationService;

@Controller
@RequestMapping("/notifications")
public class InternalNotificationController {

    private final InternalNotificationService notificationService;

    public InternalNotificationController(
            InternalNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "notifications",
                notificationService.findForUser(authentication.getName(), page));
        model.addAttribute(
                "unreadCount",
                notificationService.unreadCount(authentication.getName()));
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markRead(
            @PathVariable Long id,
            Authentication authentication) {
        return "redirect:" + notificationService.markRead(
                id,
                authentication.getName());
    }
}
