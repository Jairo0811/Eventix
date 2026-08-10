package com.jairomatias.eventix.sale.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.reservation.entity.Reservation;

class SalePlatformFeeTest {

    @Test
    void newSaleFreezesCurrentPlatformFeeRate() {
        Reservation reservation = new Reservation(
                "RSV-TEST",
                null,
                "Jairo",
                "Matías",
                "jairo@example.com",
                "8090000000",
                1,
                LocalDateTime.now().plusMinutes(15),
                null);

        Sale sale = new Sale("SAL-TEST", reservation, "DOP", null);

        assertThat(sale.getPlatformFeeRate())
                .isEqualByComparingTo(Sale.DEFAULT_PLATFORM_FEE_RATE);
        assertThat(sale.getPlatformFeeRate()).isEqualByComparingTo("0.0500");
    }
}
