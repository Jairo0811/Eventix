package com.jairomatias.eventix.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.promotion.entity.Coupon;
import com.jairomatias.eventix.promotion.entity.CouponRedemption;
import com.jairomatias.eventix.promotion.entity.CouponRedemptionStatus;
import com.jairomatias.eventix.promotion.entity.DiscountType;
import com.jairomatias.eventix.promotion.repository.CouponRedemptionRepository;
import com.jairomatias.eventix.promotion.repository.CouponRepository;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.entity.TicketTypeCategory;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;

@ExtendWith(MockitoExtension.class)
class DefaultPromotionServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 8, 10, 12, 0);

    @Mock private CouponRepository couponRepository;
    @Mock private CouponRedemptionRepository redemptionRepository;
    @Mock private Event event;
    @Mock private Reservation reservation;
    @Mock private User soldBy;

    private DefaultPromotionService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPromotionService(
                couponRepository,
                redemptionRepository);
        org.mockito.Mockito.lenient().when(event.getId()).thenReturn(8L);
        org.mockito.Mockito.lenient().when(reservation.getEvent())
                .thenReturn(event);
        org.mockito.Mockito.lenient().when(reservation.getAttendeeFullName())
                .thenReturn("María Pérez");
        org.mockito.Mockito.lenient().when(reservation.getAttendeeEmail())
                .thenReturn("Maria@Example.com");
        org.mockito.Mockito.lenient().when(reservation.getAttendeePhone())
                .thenReturn("809-555-0101");
    }

    @Test
    void calculatesPercentageFromServerSubtotalAndStoresSnapshot() {
        Sale sale = sale("999.99");
        Coupon coupon = coupon(DiscountType.PERCENTAGE, "15.00");
        when(couponRepository.findByCodeForUpdate("VERANO15"))
                .thenReturn(Optional.of(coupon));

        service.reserveForSale(" verano15 ", sale, NOW);

        assertThat(sale.getSubtotal()).isEqualByComparingTo("999.99");
        assertThat(sale.getDiscountTotal()).isEqualByComparingTo("150.00");
        assertThat(sale.getTotal()).isEqualByComparingTo("849.99");
        assertThat(sale.getCouponCode()).isEqualTo("VERANO15");
        assertThat(coupon.getCurrentUses()).isEqualTo(1);
        verify(redemptionRepository).save(any(CouponRedemption.class));
    }

    @Test
    void capsFixedDiscountSoTotalNeverBecomesNegative() {
        Sale sale = sale("500.00");
        Coupon coupon = coupon(DiscountType.FIXED_AMOUNT, "900.00");
        when(couponRepository.findByCodeForUpdate("VERANO15"))
                .thenReturn(Optional.of(coupon));

        service.reserveForSale("VERANO15", sale, NOW);

        assertThat(sale.getDiscountTotal()).isEqualByComparingTo("500.00");
        assertThat(sale.getTotal()).isZero();
    }

    @Test
    void rejectsExpiredCoupon() {
        Sale sale = sale("500.00");
        Coupon coupon = new Coupon(
                "VENCIDO",
                "Promoción vencida",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                NOW.minusDays(2),
                NOW.minusDays(1),
                true,
                null,
                null,
                null,
                Set.of(event));
        ReflectionTestUtils.setField(coupon, "id", 4L);
        when(couponRepository.findByCodeForUpdate("VENCIDO"))
                .thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> service.reserveForSale(
                "VENCIDO",
                sale,
                NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void rejectsBuyerWhoReachedPerUserLimit() {
        Sale sale = sale("500.00");
        Coupon coupon = new Coupon(
                "UNICO",
                "Una vez por comprador",
                DiscountType.PERCENTAGE,
                new BigDecimal("10.00"),
                NOW.minusDays(1),
                NOW.plusDays(1),
                true,
                20,
                1,
                null,
                Set.of(event));
        ReflectionTestUtils.setField(coupon, "id", 4L);
        when(couponRepository.findByCodeForUpdate("UNICO"))
                .thenReturn(Optional.of(coupon));
        when(redemptionRepository
                .countByCoupon_IdAndBuyerEmailIgnoreCaseAndStatusIn(
                        4L,
                        "maria@example.com",
                        java.util.List.of(
                                CouponRedemptionStatus.RESERVED,
                                CouponRedemptionStatus.CONSUMED)))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.reserveForSale(
                "UNICO",
                sale,
                NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("comprador");
    }

    @Test
    void releasesOnlyReservedUseWhenPendingSaleIsCancelled() {
        Sale sale = sale("500.00");
        ReflectionTestUtils.setField(sale, "id", 55L);
        Coupon coupon = coupon(DiscountType.PERCENTAGE, "10.00");
        coupon.reserveUse();
        CouponRedemption redemption = new CouponRedemption(
                coupon,
                sale,
                "maria@example.com",
                new BigDecimal("50.00"),
                NOW.minusMinutes(5));
        when(redemptionRepository.findBySale_Id(55L))
                .thenReturn(Optional.of(redemption));
        when(couponRepository.findByCodeForUpdate("VERANO15"))
                .thenReturn(Optional.of(coupon));

        service.releaseForSale(55L, NOW);

        assertThat(redemption.getStatus())
                .isEqualTo(CouponRedemptionStatus.RELEASED);
        assertThat(coupon.getCurrentUses()).isZero();
    }

    private Sale sale(String price) {
        Sale sale = new Sale("SAL-ABCDEFGH2345", reservation, "DOP", soldBy);
        TicketType ticketType = new TicketType(
                event,
                TicketTypeCategory.GENERAL,
                "General",
                new BigDecimal(price),
                100);
        ReflectionTestUtils.setField(ticketType, "id", 31L);
        sale.addItem(ticketType, 1);
        return sale;
    }

    private Coupon coupon(DiscountType type, String value) {
        Coupon coupon = new Coupon(
                "VERANO15",
                "Promoción de verano",
                type,
                new BigDecimal(value),
                NOW.minusDays(1),
                NOW.plusDays(1),
                true,
                50,
                null,
                new BigDecimal("100.00"),
                Set.of(event));
        ReflectionTestUtils.setField(coupon, "id", 4L);
        return coupon;
    }
}
