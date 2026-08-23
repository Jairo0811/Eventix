package com.jairomatias.eventix.sale.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.promotion.entity.Coupon;
import com.jairomatias.eventix.reservation.entity.Reservation;

class SaleDiscountTest {

    @Test
    void appliesEligibilityDiscountAndRecalculatesTotal() {
        Sale sale = saleWithTicket(new BigDecimal("1000.00"), 2);

        sale.applyEligibilityDiscount(
                99L,
                EligibilityBenefitType.PERCENTAGE_DISCOUNT,
                new BigDecimal("25.00"),
                new BigDecimal("500.00"));

        assertThat(sale.getSubtotal()).isEqualByComparingTo("2000.00");
        assertThat(sale.getEligibilityBenefitId()).isEqualTo(99L);
        assertThat(sale.getEligibilityDiscountAmount()).isEqualByComparingTo("500.00");
        assertThat(sale.getDiscountTotal()).isEqualByComparingTo("500.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("1500.00");
    }

    @Test
    void freeEntryNeverProducesNegativeTotal() {
        Sale sale = saleWithTicket(new BigDecimal("750.00"), 2);

        sale.applyEligibilityDiscount(
                100L,
                EligibilityBenefitType.FREE_ENTRY,
                null,
                new BigDecimal("1500.00"));

        assertThat(sale.getDiscountTotal()).isEqualByComparingTo("1500.00");
        assertThat(sale.getTotal()).isZero();
    }

    @Test
    void rejectsCouponStackingAfterEligibilityDiscount() {
        Sale sale = saleWithTicket(new BigDecimal("1000.00"), 1);
        sale.applyEligibilityDiscount(
                99L,
                EligibilityBenefitType.FIXED_DISCOUNT,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"));

        assertThatThrownBy(() -> sale.applyCoupon(mock(Coupon.class), new BigDecimal("50.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no pueden acumularse");
    }

    private Sale saleWithTicket(BigDecimal unitPrice, int quantity) {
        Reservation reservation = mock(Reservation.class);
        when(reservation.getAttendeeFullName()).thenReturn("Jairo Matías");
        when(reservation.getAttendeeEmail()).thenReturn("jairo@example.com");
        when(reservation.getAttendeePhone()).thenReturn("8090000000");

        TicketType ticketType = mock(TicketType.class);
        when(ticketType.getName()).thenReturn("General");
        when(ticketType.getPrice()).thenReturn(unitPrice);

        Sale sale = new Sale("SAL-TEST", reservation, "DOP", null);
        sale.addItem(ticketType, quantity);
        return sale;
    }
}
