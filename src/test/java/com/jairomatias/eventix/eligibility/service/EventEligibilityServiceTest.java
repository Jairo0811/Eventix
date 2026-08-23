package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.dto.EligibilityDiscountDecision;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.repository.EligibilityBenefitRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventAccessMode;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;

@ExtendWith(MockitoExtension.class)
class EventEligibilityServiceTest {

    @Mock
    private EligibilityMembershipRepository membershipRepository;
    @Mock
    private EligibilityBenefitRepository benefitRepository;

    private EventEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new EventEligibilityService(membershipRepository, benefitRepository);
    }

    @Test
    void allowsPublicEventWithoutMembership() {
        Event event = event(10L, EventAccessMode.PUBLIC);
        User user = user(20L);

        service.assertPurchaseAllowed(event, user, 30L, 1);
    }

    @Test
    void rejectsControlledEventWithoutVerifiedMembership() {
        Event event = event(10L, EventAccessMode.CONTROLLED_ACCESS);
        User user = user(20L);
        when(membershipRepository.findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
                10L, 20L, EligibilityMembershipStatus.VERIFIED)).thenReturn(List.of());

        assertThatThrownBy(() -> service.assertPurchaseAllowed(event, user, 30L, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("elegibilidad verificada");
    }

    @Test
    void enforcesPurchaseLimitForVerifiedMember() {
        Event event = event(10L, EventAccessMode.CONTROLLED_ACCESS);
        User user = user(20L);
        EligibilityGroup group = group(40L);
        EligibilityMembership membership = membership(group);
        EligibilityBenefit limit = benefit(50L, EligibilityBenefitType.PURCHASE_LIMIT, null, null, 2);

        when(membershipRepository.findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
                10L, 20L, EligibilityMembershipStatus.VERIFIED)).thenReturn(List.of(membership));
        when(benefitRepository.findAllByTicketType_IdAndBenefitTypeAndActiveTrue(
                30L, EligibilityBenefitType.EXCLUSIVE_TICKET)).thenReturn(List.of());
        when(benefitRepository.findAllByGroup_IdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(limit));

        assertThatThrownBy(() -> service.assertPurchaseAllowed(event, user, 30L, 3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("máximo de 2 entradas");
    }

    @Test
    void resolvesPercentageDiscountForVerifiedMember() {
        Event event = event(10L, EventAccessMode.PUBLIC);
        User user = user(20L);
        EligibilityGroup group = group(40L);
        EligibilityMembership membership = membership(group);
        EligibilityBenefit discount = benefit(
                51L,
                EligibilityBenefitType.PERCENTAGE_DISCOUNT,
                new BigDecimal("25.00"),
                null,
                null);

        when(membershipRepository.findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
                10L, 20L, EligibilityMembershipStatus.VERIFIED)).thenReturn(List.of(membership));
        when(benefitRepository.findAllByGroup_IdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(discount));

        EligibilityDiscountDecision decision = service.resolveMonetaryDiscount(
                        event, user, 30L, new BigDecimal("1000.00"))
                .orElseThrow();

        assertThat(decision.benefitId()).isEqualTo(51L);
        assertThat(decision.benefitType()).isEqualTo(EligibilityBenefitType.PERCENTAGE_DISCOUNT);
        assertThat(decision.discountAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    void selectsMostFavorableApplicableMonetaryBenefit() {
        Event event = event(10L, EventAccessMode.PUBLIC);
        User user = user(20L);
        EligibilityGroup group = group(40L);
        EligibilityMembership membership = membership(group);
        EligibilityBenefit percentage = benefit(
                51L,
                EligibilityBenefitType.PERCENTAGE_DISCOUNT,
                new BigDecimal("10.00"),
                null,
                null);
        EligibilityBenefit fixed = benefit(
                52L,
                EligibilityBenefitType.FIXED_DISCOUNT,
                new BigDecimal("300.00"),
                null,
                null);

        when(membershipRepository.findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
                10L, 20L, EligibilityMembershipStatus.VERIFIED)).thenReturn(List.of(membership));
        when(benefitRepository.findAllByGroup_IdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(percentage, fixed));

        EligibilityDiscountDecision decision = service.resolveMonetaryDiscount(
                        event, user, 30L, new BigDecimal("1000.00"))
                .orElseThrow();

        assertThat(decision.benefitId()).isEqualTo(52L);
        assertThat(decision.discountAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void ignoresTicketSpecificDiscountForAnotherTicketType() {
        Event event = event(10L, EventAccessMode.PUBLIC);
        User user = user(20L);
        EligibilityGroup group = group(40L);
        EligibilityMembership membership = membership(group);
        TicketType otherTicket = mock(TicketType.class);
        when(otherTicket.getId()).thenReturn(99L);
        EligibilityBenefit discount = benefit(
                51L,
                EligibilityBenefitType.FIXED_DISCOUNT,
                new BigDecimal("300.00"),
                otherTicket,
                null);

        when(membershipRepository.findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
                10L, 20L, EligibilityMembershipStatus.VERIFIED)).thenReturn(List.of(membership));
        when(benefitRepository.findAllByGroup_IdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(discount));

        assertThat(service.resolveMonetaryDiscount(
                event, user, 30L, new BigDecimal("1000.00"))).isEmpty();
    }

    private Event event(Long id, EventAccessMode accessMode) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(id);
        when(event.getAccessMode()).thenReturn(accessMode);
        return event;
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private EligibilityGroup group(Long id) {
        EligibilityGroup group = mock(EligibilityGroup.class);
        when(group.getId()).thenReturn(id);
        return group;
    }

    private EligibilityMembership membership(EligibilityGroup group) {
        EligibilityMembership membership = mock(EligibilityMembership.class);
        when(membership.getGroup()).thenReturn(group);
        return membership;
    }

    private EligibilityBenefit benefit(
            Long id,
            EligibilityBenefitType type,
            BigDecimal discountValue,
            TicketType ticketType,
            Integer maxTickets) {
        EligibilityBenefit benefit = mock(EligibilityBenefit.class);
        lenient().when(benefit.getId()).thenReturn(id);
        lenient().when(benefit.getBenefitType()).thenReturn(type);
        lenient().when(benefit.getDiscountValue()).thenReturn(discountValue);
        lenient().when(benefit.getTicketType()).thenReturn(ticketType);
        lenient().when(benefit.getMaxTicketsPerPurchase()).thenReturn(maxTickets);
        return benefit;
    }
}
