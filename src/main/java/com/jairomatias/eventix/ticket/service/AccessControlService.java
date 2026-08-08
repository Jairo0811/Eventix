package com.jairomatias.eventix.ticket.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.ticket.dto.AccessDashboardSummary;
import com.jairomatias.eventix.ticket.dto.ScanAttemptListItem;
import com.jairomatias.eventix.ticket.dto.ScanForm;
import com.jairomatias.eventix.ticket.dto.ScanResultView;

public interface AccessControlService {

    ScanResultView scan(
            ScanForm form,
            String authenticatedLogin,
            String ipAddress);

    Page<ScanAttemptListItem> findAttempts(
            Long eventId,
            String authenticatedLogin,
            Pageable pageable);

    AccessDashboardSummary getSummary(
            Long eventId,
            String authenticatedLogin);

    List<EventReservationOption> findVisibleEvents(
            String authenticatedLogin);
}
