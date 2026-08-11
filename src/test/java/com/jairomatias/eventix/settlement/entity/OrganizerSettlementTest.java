package com.jairomatias.eventix.settlement.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.user.entity.User;

@ExtendWith(MockitoExtension.class)
class OrganizerSettlementTest {

    @Mock private User organizer;
    @Mock private Sale sale;

    @Test
    void saleAndRefundProduceAuditableZeroNetAdjustment() {
        prepareSaleAmounts();
        OrganizerSettlement settlement = settlement();

        settlement.addSale(sale);
        settlement.addRefund(sale);

        assertThat(settlement.getGrossSales()).isEqualByComparingTo("1000.00");
        assertThat(settlement.getDiscounts()).isEqualByComparingTo("100.00");
        assertThat(settlement.getRefunds()).isEqualByComparingTo("900.00");
        assertThat(settlement.getPlatformCommission()).isZero();
        assertThat(settlement.getOrganizerNet()).isZero();
        assertThat(settlement.getLines()).hasSize(2);
    }

    @Test
    void paidTransitionRequiresProcessingState() {
        OrganizerSettlement settlement = settlement();

        assertThatThrownBy(() -> settlement.markPaid(
                LocalDateTime.now(),
                "BANK-100",
                null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("procesamiento");
    }

    @Test
    void cancellingKeepsHistoryButReleasesEveryLine() {
        prepareSaleAmounts();
        OrganizerSettlement settlement = settlement();
        settlement.addSale(sale);

        settlement.cancel("Período incorrecto");

        assertThat(settlement.getStatus())
                .isEqualTo(SettlementStatus.CANCELLED);
        assertThat(settlement.getLines()).allMatch(line -> !line.isActive());
        assertThat(settlement.getAdministrativeNotes())
                .isEqualTo("Período incorrecto");
    }

    private OrganizerSettlement settlement() {
        return new OrganizerSettlement(
                organizer,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null);
    }

    private void prepareSaleAmounts() {
        when(sale.getSubtotal()).thenReturn(new BigDecimal("1000.00"));
        when(sale.getDiscountTotal()).thenReturn(new BigDecimal("100.00"));
        when(sale.getTotal()).thenReturn(new BigDecimal("900.00"));
        when(sale.getPlatformFeeAmount()).thenReturn(new BigDecimal("45.00"));
        when(sale.getOrganizerNetAmount()).thenReturn(new BigDecimal("855.00"));
    }
}
