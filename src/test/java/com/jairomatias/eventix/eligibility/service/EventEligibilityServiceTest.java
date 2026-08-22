package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.repository.EligibilityBenefitRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventAccessMode;
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
        EligibilityBenefit limit = benefit(EligibilityBenefitType.PURCHASE_LIMIT, 2);

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

    private EligibilityBenefit benefit(EligibilityBenefitType type, Integer maxTickets) {
        EligibilityBenefit benefit = mock(EligibilityBenefit.class);
        when(benefit.getBenefitType()).thenReturn(type);
        when(benefit.getMaxTicketsPerPurchase()).thenReturn(maxTickets);
        return benefit;
    }
}
