package com.jairomatias.eventix.eligibility.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.repository.EligibilityBenefitRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventAccessMode;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;

@Service
public class EventEligibilityService {

    private final EligibilityMembershipRepository membershipRepository;
    private final EligibilityBenefitRepository benefitRepository;

    public EventEligibilityService(
            EligibilityMembershipRepository membershipRepository,
            EligibilityBenefitRepository benefitRepository) {
        this.membershipRepository = membershipRepository;
        this.benefitRepository = benefitRepository;
    }

    @Transactional(readOnly = true)
    public void assertEventAccess(Event event, User user) {
        if (event.getAccessMode() == EventAccessMode.PUBLIC) {
            return;
        }
        if (verifiedMemberships(event, user).isEmpty()) {
            throw new BusinessRuleException(
                    "No tienes una elegibilidad verificada para acceder a este evento.");
        }
    }

    @Transactional(readOnly = true)
    public boolean isTicketVisible(Event event, User user, Long ticketTypeId) {
        try {
            assertPurchaseAllowed(event, user, ticketTypeId, 1);
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void assertPurchaseAllowed(
            Event event,
            User user,
            Long ticketTypeId,
            int quantity) {
        assertEventAccess(event, user);

        List<EligibilityMembership> memberships = verifiedMemberships(event, user);
        Set<Long> groupIds = memberships.stream()
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toSet());

        enforceExclusiveTicket(groupIds, ticketTypeId);
        if (groupIds.isEmpty()) {
            return;
        }

        List<EligibilityBenefit> benefits = benefitRepository.findAllByGroup_IdInAndActiveTrue(groupIds);
        enforcePurchaseLimit(benefits, quantity);
    }

    private List<EligibilityMembership> verifiedMemberships(Event event, User user) {
        return membershipRepository.findAllByGroup_Event_IdAndUser_IdAndStatusAndActiveTrue(
                event.getId(), user.getId(), EligibilityMembershipStatus.VERIFIED);
    }

    private void enforcePurchaseLimit(List<EligibilityBenefit> benefits, int quantity) {
        Integer strictestLimit = benefits.stream()
                .filter(benefit -> benefit.getBenefitType() == EligibilityBenefitType.PURCHASE_LIMIT)
                .map(EligibilityBenefit::getMaxTicketsPerPurchase)
                .filter(limit -> limit != null)
                .min(Integer::compareTo)
                .orElse(null);
        if (strictestLimit != null && quantity > strictestLimit) {
            throw new BusinessRuleException(
                    "La elegibilidad aplicada permite un máximo de " + strictestLimit + " entradas por compra.");
        }
    }

    private void enforceExclusiveTicket(Set<Long> groupIds, Long ticketTypeId) {
        List<EligibilityBenefit> restrictions = benefitRepository
                .findAllByTicketType_IdAndBenefitTypeAndActiveTrue(
                        ticketTypeId, EligibilityBenefitType.EXCLUSIVE_TICKET);
        if (restrictions.isEmpty()) {
            return;
        }
        boolean allowed = restrictions.stream()
                .map(benefit -> benefit.getGroup().getId())
                .anyMatch(groupIds::contains);
        if (!allowed) {
            throw new BusinessRuleException(
                    "El tipo de entrada seleccionado requiere una elegibilidad específica.");
        }
    }
}
