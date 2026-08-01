package com.jairomatias.eventix.reservation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            ReservationExpirationScheduler.class);

    private final ReservationService reservationService;

    public ReservationExpirationScheduler(
            ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            fixedDelayString =
                    "${eventix.reservations.expiration-scan-interval:PT1M}")
    public void expirePendingReservations() {
        int expired = reservationService.expirePendingReservations();
        if (expired > 0) {
            LOGGER.info(
                    "Se liberaron {} reservaciones expiradas.",
                    expired);
        }
    }
}
