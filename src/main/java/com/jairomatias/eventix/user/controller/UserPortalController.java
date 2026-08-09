package com.jairomatias.eventix.user.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jairomatias.eventix.security.UserPrincipal;
import com.jairomatias.eventix.ticket.service.TicketService;

@Controller
@RequestMapping("/my")
@PreAuthorize("hasRole('USER')")
public class UserPortalController {

    private static final int TICKET_PAGE_SIZE = 12;

    private final TicketService ticketService;

    public UserPortalController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public String home(
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        model.addAttribute("principal", principal);
        model.addAttribute(
                "tickets",
                ticketService.findMine(
                        principal.getUsername(),
                        PageRequest.of(
                                0,
                                6,
                                Sort.by("issuedAt").descending())));
        return "user-portal/home";
    }

    @GetMapping("/tickets")
    public String tickets(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        model.addAttribute("principal", principal);
        model.addAttribute(
                "tickets",
                ticketService.findMine(
                        principal.getUsername(),
                        PageRequest.of(
                                Math.max(page, 0),
                                TICKET_PAGE_SIZE,
                                Sort.by("issuedAt").descending())));
        return "user-portal/tickets";
    }

    @GetMapping("/profile")
    public String profile(
            @AuthenticationPrincipal UserPrincipal principal,
            Model model) {
        model.addAttribute("principal", principal);
        return "user-portal/profile";
    }
}
