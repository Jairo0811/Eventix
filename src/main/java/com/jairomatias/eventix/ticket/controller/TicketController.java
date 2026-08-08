package com.jairomatias.eventix.ticket.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jairomatias.eventix.ticket.dto.TicketDetailsView;
import com.jairomatias.eventix.ticket.dto.TicketListItem;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.service.TicketService;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private static final int PAGE_SIZE = 15;
    private static final MediaType PASSBOOK =
            MediaType.parseMediaType("application/vnd.apple.pkpass");

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication,
            Model model) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                PAGE_SIZE,
                Sort.by("issuedAt").descending());
        Page<TicketListItem> tickets = ticketService.findAll(
                term,
                status,
                eventId,
                authentication.getName(),
                pageable);
        model.addAttribute("tickets", tickets);
        model.addAttribute(
                "summary",
                ticketService.getSummary(eventId, authentication.getName()));
        model.addAttribute(
                "events",
                ticketService.findVisibleEvents(authentication.getName()));
        model.addAttribute("ticketStatuses", TicketStatus.values());
        model.addAttribute("term", term);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedEventId", eventId);
        return "tickets/list";
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        model.addAttribute(
                "ticket",
                ticketService.findById(id, authentication.getName()));
        return "tickets/detail";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(
            @PathVariable Long id,
            Authentication authentication) {
        TicketDetailsView ticket = ticketService.findById(
                id,
                authentication.getName());
        return download(
                ticketService.createPdf(id, authentication.getName()),
                MediaType.APPLICATION_PDF,
                "eventix-" + ticket.uniqueCode() + ".pdf");
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> qr(
            @PathVariable Long id,
            Authentication authentication) {
        TicketDetailsView ticket = ticketService.findById(
                id,
                authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        "qr-" + ticket.uniqueCode() + ".png")
                                .build()
                                .toString())
                .body(ticketService.createQrPng(
                        id,
                        authentication.getName()));
    }

    @GetMapping("/{id}/wallet/google")
    public ResponseEntity<Void> googleWallet(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.status(302)
                .location(URI.create(ticketService.createGoogleWalletUrl(
                        id,
                        authentication.getName())))
                .build();
    }

    @GetMapping("/{id}/wallet/apple")
    public ResponseEntity<byte[]> appleWallet(
            @PathVariable Long id,
            Authentication authentication) {
        TicketDetailsView ticket = ticketService.findById(
                id,
                authentication.getName());
        return download(
                ticketService.createAppleWalletPass(
                        id,
                        authentication.getName()),
                PASSBOOK,
                "eventix-" + ticket.uniqueCode() + ".pkpass");
    }

    private ResponseEntity<byte[]> download(
            byte[] body,
            MediaType contentType,
            String filename) {
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(body);
    }
}
