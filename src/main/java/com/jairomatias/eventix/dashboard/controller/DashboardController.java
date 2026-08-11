package com.jairomatias.eventix.dashboard.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jairomatias.eventix.dashboard.service.DashboardService;
import com.jairomatias.eventix.dashboard.service.OrganizerDashboardService;
import com.jairomatias.eventix.security.UserPrincipal;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final OrganizerDashboardService organizerDashboardService;

    public DashboardController(
            DashboardService dashboardService,
            OrganizerDashboardService organizerDashboardService) {
        this.dashboardService = dashboardService;
        this.organizerDashboardService = organizerDashboardService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize(
            "hasAnyRole('ADMINISTRATOR', 'ORGANIZER', 'OPERATOR', 'ACCESS_STAFF')")
    public String dashboard(
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        model.addAttribute("principal", principal);
        if ("ADMINISTRATOR".equals(principal.getRoleName())) {
            model.addAttribute("summary", dashboardService.getSummary());
        } else if ("ORGANIZER".equals(principal.getRoleName())) {
            model.addAttribute(
                    "organizerSummary",
                    organizerDashboardService.getSummary(
                            principal.getUsername()));
        }
        return "dashboard/index";
    }
}
