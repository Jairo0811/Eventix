package com.jairomatias.eventix.notification.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.jairomatias.eventix.auth.event.PasswordResetRequestedEvent;
import com.jairomatias.eventix.notification.service.NotificationService;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.reservation.event.ReservationCancelledEvent;
import com.jairomatias.eventix.reservation.event.ReservationConfirmedEvent;
import com.jairomatias.eventix.reservation.repository.ReservationRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.event.SaleRefundedEvent;
import com.jairomatias.eventix.sale.repository.SaleRepository;

@Component
public class TransactionalNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            TransactionalNotificationListener.class);

    private final NotificationService notificationService;
    private final ReservationRepository reservationRepository;
    private final SaleRepository saleRepository;

    public TransactionalNotificationListener(
            NotificationService notificationService,
            ReservationRepository reservationRepository,
            SaleRepository saleRepository) {
        this.notificationService = notificationService;
        this.reservationRepository = reservationRepository;
        this.saleRepository = saleRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        reservationRepository.findDetailedById(event.reservationId())
                .ifPresent(reservation -> safelyNotify(() ->
                        sendReservationConfirmation(reservation),
                        "reservation-confirmed",
                        event.reservationId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCancelled(ReservationCancelledEvent event) {
        reservationRepository.findDetailedById(event.reservationId())
                .ifPresent(reservation -> safelyNotify(() ->
                        sendReservationCancellation(reservation),
                        "reservation-cancelled",
                        event.reservationId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSalePaid(SalePaidEvent event) {
        saleRepository.findDetailedById(event.saleId())
                .ifPresent(sale -> safelyNotify(() ->
                        sendPurchaseConfirmation(sale),
                        "sale-paid",
                        event.saleId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleRefunded(SaleRefundedEvent event) {
        saleRepository.findDetailedById(event.saleId())
                .ifPresent(sale -> safelyNotify(() ->
                        sendRefundConfirmation(sale),
                        "sale-refunded",
                        event.saleId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        safelyNotify(
                () -> notificationService.sendPasswordReset(
                        event.email(),
                        event.resetUrl()),
                "password-reset",
                null);
    }

    private void sendReservationConfirmation(Reservation reservation) {
        notificationService.sendReservationConfirmation(
                reservation.getAttendeeEmail(),
                reservation.getReferenceCode());
    }

    private void sendReservationCancellation(Reservation reservation) {
        notificationService.sendCancellation(
                reservation.getAttendeeEmail(),
                reservation.getReferenceCode());
    }

    private void sendPurchaseConfirmation(Sale sale) {
        notificationService.sendPurchaseConfirmation(
                sale.getBuyerEmail(),
                sale.getReferenceCode());
    }

    private void sendRefundConfirmation(Sale sale) {
        notificationService.sendRefundConfirmation(
                sale.getBuyerEmail(),
                sale.getReferenceCode());
    }

    private void safelyNotify(
            Runnable notification,
            String notificationType,
            Long aggregateId) {
        try {
            notification.run();
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Unable to deliver {} notification for aggregate {}.",
                    notificationType,
                    aggregateId,
                    exception);
        }
    }
}
