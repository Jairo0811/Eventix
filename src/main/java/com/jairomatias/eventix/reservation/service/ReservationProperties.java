package com.jairomatias.eventix.reservation.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eventix.reservations")
public class ReservationProperties {

    private Duration holdDuration = Duration.ofMinutes(15);
    private Duration expirationScanInterval = Duration.ofMinutes(1);

    public Duration getHoldDuration() {
        return holdDuration;
    }

    public void setHoldDuration(Duration holdDuration) {
        this.holdDuration = requirePositive(
                holdDuration,
                "hold-duration");
    }

    public Duration getExpirationScanInterval() {
        return expirationScanInterval;
    }

    public void setExpirationScanInterval(
            Duration expirationScanInterval) {
        this.expirationScanInterval = requirePositive(
                expirationScanInterval,
                "expiration-scan-interval");
    }

    private Duration requirePositive(
            Duration value,
            String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "eventix.reservations."
                            + propertyName
                            + " debe ser mayor que cero.");
        }
        return value;
    }
}
