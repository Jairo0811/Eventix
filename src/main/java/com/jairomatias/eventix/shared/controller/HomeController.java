package com.jairomatias.eventix.shared.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.jairomatias.eventix.home.service.EventDiscoveryService;
import com.jairomatias.eventix.home.service.PublicStorefrontService;

@Controller
public class HomeController {

    /** Categorías mostradas como accesos rápidos en la portada. */
    private static final int HOME_CATEGORY_LIMIT = 6;

    private final EventDiscoveryService eventDiscoveryService;
    private final PublicStorefrontService storefront;
    private final String baseUrl;

    public HomeController(
            EventDiscoveryService eventDiscoveryService,
            PublicStorefrontService storefront,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.eventDiscoveryService = eventDiscoveryService;
        this.storefront = storefront;
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
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
        model.addAttribute(
                "categories",
                storefront.categories().stream().limit(HOME_CATEGORY_LIMIT).toList());
        model.addAttribute("canonicalUrl", baseUrl + "/");
        model.addAttribute("ogImage", baseUrl + "/images/eventix-share.png");
        return "home/index";
    }
}
