package com.jairomatias.eventix.sale.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.payment.dto.PaymentForm;
import com.jairomatias.eventix.reservation.dto.EventReservationOption;
import com.jairomatias.eventix.sale.dto.ReservationSaleOption;
import com.jairomatias.eventix.sale.dto.SaleActionForm;
import com.jairomatias.eventix.sale.dto.SaleDetailsView;
import com.jairomatias.eventix.sale.dto.SaleForm;
import com.jairomatias.eventix.sale.dto.SaleListItem;
import com.jairomatias.eventix.sale.dto.SalesSummary;
import com.jairomatias.eventix.sale.dto.TicketTypeOption;
import com.jairomatias.eventix.sale.entity.SaleStatus;

public interface SaleService {

    Page<SaleListItem> findAll(
            String term,
            SaleStatus status,
            Long eventId,
            String authenticatedLogin,
            Pageable pageable);

    SaleDetailsView findById(Long id, String authenticatedLogin);

    SalesSummary getSummary(String authenticatedLogin);

    List<EventReservationOption> findVisibleEvents(String authenticatedLogin);

    List<ReservationSaleOption> findSaleableReservations();

    SaleForm getCreateForm(Long reservationId, String authenticatedLogin);

    List<TicketTypeOption> findTicketTypeOptions(
            Long reservationId,
            String authenticatedLogin);

    Long create(SaleForm form, String authenticatedLogin);

    boolean processPayment(
            Long id,
            PaymentForm form,
            String authenticatedLogin);

    void refund(
            Long id,
            SaleActionForm form,
            String authenticatedLogin);

    void cancel(
            Long id,
            SaleActionForm form,
            String authenticatedLogin);
}
