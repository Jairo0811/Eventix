package com.jairomatias.eventix.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.settlement.dto.SettlementCreateForm;
import com.jairomatias.eventix.settlement.entity.OrganizerSettlement;
import com.jairomatias.eventix.settlement.repository.OrganizerSettlementRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DefaultOrganizerSettlementServiceTest {

    @Mock private OrganizerSettlementRepository settlementRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private UserRepository userRepository;
    @Mock private User organizer;
    @Mock private Role organizerRole;
    @Mock private User otherOrganizer;
    @Mock private Role otherOrganizerRole;
    @Mock private Sale sale;

    private DefaultOrganizerSettlementService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-10T12:00:00Z"),
                ZoneOffset.UTC);
        service = new DefaultOrganizerSettlementService(
                settlementRepository,
                saleRepository,
                userRepository,
                clock);
    }

    @Test
    void createsSettlementFromOnlyUnsettledFinancialEffects() {
        prepareOrganizer();
        prepareSaleAmounts();
        when(userRepository.findById(8L)).thenReturn(Optional.of(organizer));
        when(saleRepository.findUnsettledSalesForUpdate(
                any(), any(), any(), any()))
                .thenReturn(List.of(sale));
        when(saleRepository.findUnsettledRefundsForUpdate(
                any(), any(), any()))
                .thenReturn(List.of());
        when(settlementRepository.save(any(OrganizerSettlement.class)))
                .thenAnswer(invocation -> {
                    OrganizerSettlement settlement = invocation.getArgument(0);
                    ReflectionTestUtils.setField(settlement, "id", 44L);
                    return settlement;
                });

        Long id = service.create(validForm());

        assertThat(id).isEqualTo(44L);
        verify(saleRepository).findUnsettledSalesForUpdate(
                org.mockito.ArgumentMatchers.eq(8L),
                any(),
                org.mockito.ArgumentMatchers.eq(
                        LocalDate.of(2026, 7, 1).atStartOfDay()),
                org.mockito.ArgumentMatchers.eq(
                        LocalDate.of(2026, 8, 1).atStartOfDay()));
    }

    @Test
    void rejectsPeriodWithoutEligibleMovements() {
        prepareOrganizer();
        when(userRepository.findById(8L)).thenReturn(Optional.of(organizer));
        when(saleRepository.findUnsettledSalesForUpdate(
                any(), any(), any(), any()))
                .thenReturn(List.of());
        when(saleRepository.findUnsettledRefundsForUpdate(
                any(), any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(validForm()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pendientes");
    }

    @Test
    void rejectsFutureSettlementPeriod() {
        SettlementCreateForm form = validForm();
        form.setPeriodTo(LocalDate.of(2026, 8, 11));

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("futuros");
    }

    @Test
    void organizerCannotReadAnotherOrganizersSettlementByChangingId() {
        OrganizerSettlement settlement = org.mockito.Mockito.mock(
                OrganizerSettlement.class);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "other@example.com",
                "other@example.com"))
                .thenReturn(Optional.of(otherOrganizer));
        when(otherOrganizer.getRole()).thenReturn(otherOrganizerRole);
        when(otherOrganizerRole.getName()).thenReturn(RoleName.ORGANIZER);
        when(otherOrganizer.getId()).thenReturn(99L);
        when(settlementRepository.findDetailedById(44L))
                .thenReturn(Optional.of(settlement));
        when(settlement.getOrganizer()).thenReturn(organizer);
        when(organizer.getId()).thenReturn(8L);

        assertThatThrownBy(() -> service.findById(
                44L,
                "other@example.com"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permiso");
    }

    private SettlementCreateForm validForm() {
        SettlementCreateForm form = new SettlementCreateForm();
        form.setOrganizerId(8L);
        form.setPeriodFrom(LocalDate.of(2026, 7, 1));
        form.setPeriodTo(LocalDate.of(2026, 7, 31));
        return form;
    }

    private void prepareOrganizer() {
        when(organizer.getId()).thenReturn(8L);
        when(organizer.getRole()).thenReturn(organizerRole);
        when(organizerRole.getName()).thenReturn(RoleName.ORGANIZER);
        when(organizer.getStatus()).thenReturn(UserStatus.ACTIVE);
    }

    private void prepareSaleAmounts() {
        when(sale.getSubtotal()).thenReturn(new BigDecimal("1000.00"));
        when(sale.getDiscountTotal()).thenReturn(new BigDecimal("100.00"));
        when(sale.getPlatformFeeAmount()).thenReturn(new BigDecimal("45.00"));
        when(sale.getOrganizerNetAmount()).thenReturn(new BigDecimal("855.00"));
    }
}
