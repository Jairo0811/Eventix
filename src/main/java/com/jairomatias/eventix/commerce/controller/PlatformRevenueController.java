package com.jairomatias.eventix.commerce.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jairomatias.eventix.commerce.service.PlatformRevenueService;

@Controller
public class PlatformRevenueController {

    private final PlatformRevenueService revenueService;

    public PlatformRevenueController(PlatformRevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/commerce/revenue")
    public String revenue(Authentication authentication, Model model) {
        model.addAttribute(
                "revenue",
                revenueService.getRevenue(authentication.getName()));
        return "commerce/revenue";
    }
}
