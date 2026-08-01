package com.jairomatias.eventix.reservation.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.reservation.dto.CancellationForm;
import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.reservation.dto.ReservationDetailsView;
import com.jairomatias.eventix.reservation.dto.ReservationForm;
import com.jairomatias.eventix.reservation.dto.ReservationListItem;
import com.jairomatias.eventix.reservation.dto.ReservationMetrics;
import com.jairomatias.eventix.reservation.entity.ReservationStatus;

public interface ReservationService {

    Page<ReservationListItem> findAll(
            String term,
            ReservationStatus status,
            Long eventId,
            String authenticatedLogin,
            Pageable pageable);

    ReservationDetailsView findById(
            Long id,
            String authenticatedLogin);

    ReservationForm getCreateForm(
            Long eventId,
            String authenticatedLogin);

    ReservationForm getUpdateForm(
            Long id,
            String authenticatedLogin);

    List<EventReservationOption> findVisibleEvents(
            String authenticatedLogin);

    List<EventReservationOption> findReservableEvents();

    Long create(
            ReservationForm form,
            String authenticatedLogin);

    void update(
            Long id,
            ReservationForm form,
            String authenticatedLogin);

    void confirm(
            Long id,
            String authenticatedLogin);

    void cancel(
            Long id,
            CancellationForm form,
            String authenticatedLogin);

    ReservationMetrics getEventMetrics(
            Long eventId,
            String authenticatedLogin);

    int expirePendingReservations();
}
