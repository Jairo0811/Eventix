package com.jairomatias.eventix.notification.listener;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.notification.service.NotificationService;
import com.jairomatias.eventix.notification.service.NotificationPreferenceService;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.event.ReservationCancelledEvent;
import com.jairomatias.eventix.reservation.event.ReservationConfirmedEvent;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.event.SaleRefundedEvent;
import com.jairomatias.eventix.sale.repository.SaleRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalNotificationListenerTest {

    private NotificationService notificationService;
    private NotificationPreferenceService preferenceService;
    private ReservationRepository reservationRepository;
    private SaleRepository saleRepository;
    private TransactionalNotificationListener listener;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        preferenceService = mock(NotificationPreferenceService.class);
        reservationRepository = mock(ReservationRepository.class);
        saleRepository = mock(SaleRepository.class);
        listener = new TransactionalNotificationListener(
                notificationService,
                preferenceService,
                reservationRepository,
                saleRepository);
        when(preferenceService.allowsReservationNotifications(
                anyString()))
                .thenReturn(true);
    }

    @Test
    void shouldNotifyWhenReservationIsConfirmed() {
        Reservation reservation = reservation("RSV-100", "guest@example.com");
        when(reservationRepository.findDetailedById(10L))
                .thenReturn(Optional.of(reservation));

        listener.onReservationConfirmed(new ReservationConfirmedEvent(10L));

        verify(notificationService).sendReservationConfirmation(
                "guest@example.com",
                "RSV-100");
    }

    @Test
    void shouldNotifyWhenReservationIsCancelled() {
        Reservation reservation = reservation("RSV-101", "guest@example.com");
        when(reservationRepository.findDetailedById(11L))
                .thenReturn(Optional.of(reservation));

        listener.onReservationCancelled(new ReservationCancelledEvent(11L));

        verify(notificationService).sendCancellation(
                "guest@example.com",
                "RSV-101");
    }

    @Test
    void shouldRespectReservationNotificationPreference() {
        Reservation reservation = reservation("RSV-102", "user@example.com");
        when(reservationRepository.findDetailedById(12L))
                .thenReturn(Optional.of(reservation));
        when(preferenceService.allowsReservationNotifications(
                "user@example.com"))
                .thenReturn(false);

        listener.onReservationConfirmed(new ReservationConfirmedEvent(12L));

        verify(notificationService, never()).sendReservationConfirmation(
                "user@example.com",
                "RSV-102");
    }

    @Test
    void shouldNotifyWhenSaleIsPaid() {
        Sale sale = sale("SALE-100", "buyer@example.com");
        when(saleRepository.findDetailedById(20L))
                .thenReturn(Optional.of(sale));

        listener.onSalePaid(new SalePaidEvent(20L));

        verify(notificationService).sendPurchaseConfirmation(
                "buyer@example.com",
                "SALE-100");
    }

    @Test
    void shouldNotifyWhenSaleIsRefunded() {
        Sale sale = sale("SALE-101", "buyer@example.com");
        when(saleRepository.findDetailedById(21L))
                .thenReturn(Optional.of(sale));

        listener.onSaleRefunded(new SaleRefundedEvent(21L, "Customer request"));

        verify(notificationService).sendRefundConfirmation(
                "buyer@example.com",
                "SALE-101");
    }

    @Test
    void deliveryFailureShouldNotEscapeAfterCommitListener() {
        Sale sale = sale("SALE-102", "buyer@example.com");
        when(saleRepository.findDetailedById(22L))
                .thenReturn(Optional.of(sale));
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(notificationService)
                .sendPurchaseConfirmation("buyer@example.com", "SALE-102");

        assertDoesNotThrow(() -> listener.onSalePaid(new SalePaidEvent(22L)));
    }

    private Reservation reservation(String reference, String email) {
        Reservation reservation = mock(Reservation.class);
        when(reservation.getReferenceCode()).thenReturn(reference);
        when(reservation.getAttendeeEmail()).thenReturn(email);
        return reservation;
    }

    private Sale sale(String reference, String email) {
        Sale sale = mock(Sale.class);
        when(sale.getReferenceCode()).thenReturn(reference);
        when(sale.getBuyerEmail()).thenReturn(email);
        return sale;
    }
}
