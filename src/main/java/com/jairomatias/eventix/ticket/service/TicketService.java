package com.jairomatias.eventix.ticket.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.ticket.dto.TicketDetailsView;
import com.jairomatias.eventix.ticket.dto.TicketListItem;
import com.jairomatias.eventix.ticket.dto.TicketSummary;
import com.jairomatias.eventix.ticket.entity.TicketStatus;

public interface TicketService {

    Page<TicketListItem> findAll(
            String term,
            TicketStatus status,
            Long eventId,
            String authenticatedLogin,
            Pageable pageable);

    Page<TicketListItem> findMine(
            String authenticatedLogin,
            Pageable pageable);

    TicketSummary getSummary(
            Long eventId,
            String authenticatedLogin);

    TicketDetailsView findById(
            Long id,
            String authenticatedLogin);

    List<TicketListItem> findBySale(
            Long saleId,
            String authenticatedLogin);

    List<EventReservationOption> findVisibleEvents(
            String authenticatedLogin);

    byte[] createPdf(
            Long id,
            String authenticatedLogin);

    byte[] createQrPng(
            Long id,
            String authenticatedLogin);

    String createGoogleWalletUrl(
            Long id,
            String authenticatedLogin);

    byte[] createAppleWalletPass(
            Long id,
            String authenticatedLogin);
}
