package com.jairomatias.eventix.dashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jairomatias.eventix.dashboard.service.DashboardService;
import com.jairomatias.eventix.security.UserPrincipal;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        model.addAttribute("principal", principal);
        if ("ADMINISTRATOR".equals(principal.getRoleName())) {
            model.addAttribute("summary", dashboardService.getSummary());
        }
        return "dashboard/index";
    }
}
