package com.jairomatias.eventix.shared.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jairomatias.eventix.home.service.EventDiscoveryService;

@Controller
public class HomeController {

    private final EventDiscoveryService eventDiscoveryService;

    public HomeController(EventDiscoveryService eventDiscoveryService) {
        this.eventDiscoveryService = eventDiscoveryService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        var upcoming = eventDiscoveryService.upcomingEvents();

        model.addAttribute("authenticated", authenticated);
        model.addAttribute("featuredEvent", upcoming.isEmpty() ? null : upcoming.getFirst());
        model.addAttribute("upcomingEvents", upcoming.stream().skip(1).limit(7).toList());
        model.addAttribute("nextSevenEvents", eventDiscoveryService.nextSevenEvents());
        return "home/index";
    }
}
