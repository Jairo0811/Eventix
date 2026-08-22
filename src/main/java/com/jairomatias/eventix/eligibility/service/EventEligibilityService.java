package com.jairomatias.eventix.eligibility.service;

import java.time.LocalDateTime;
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
    public boolean isTicketVisible(Event event, User user, Long ticketTypeId, LocalDateTime at) {
        try {
            assertPurchaseAllowed(event, user, ticketTypeId, 1, at);
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
            int quantity,
            LocalDateTime at) {
        assertEventAccess(event, user);

        List<EligibilityMembership> memberships = verifiedMemberships(event, user);
        if (memberships.isEmpty()) {
            return;
        }

        Set<Long> groupIds = memberships.stream()
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toSet());
        List<EligibilityBenefit> benefits = benefitRepository.findAllByGroup_IdInAndActiveTrue(groupIds);

        enforcePurchaseLimit(benefits, quantity);
        enforceExclusiveTicket(benefits, ticketTypeId);
        enforceEarlyAccess(benefits, at);
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

    private void enforceExclusiveTicket(List<EligibilityBenefit> benefits, Long ticketTypeId) {
        boolean hasExclusiveBenefits = benefits.stream()
                .anyMatch(benefit -> benefit.getBenefitType() == EligibilityBenefitType.EXCLUSIVE_TICKET);
        if (!hasExclusiveBenefits) {
            return;
        }
        boolean ticketAllowed = benefits.stream()
                .filter(benefit -> benefit.getBenefitType() == EligibilityBenefitType.EXCLUSIVE_TICKET)
                .map(EligibilityBenefit::getTicketType)
                .filter(ticketType -> ticketType != null)
                .anyMatch(ticketType -> ticketType.getId().equals(ticketTypeId));
        if (!ticketAllowed) {
            throw new BusinessRuleException(
                    "El tipo de entrada seleccionado no está habilitado para tu grupo de elegibilidad.");
        }
    }

    private void enforceEarlyAccess(List<EligibilityBenefit> benefits, LocalDateTime at) {
        LocalDateTime earliestAccess = benefits.stream()
                .filter(benefit -> benefit.getBenefitType() == EligibilityBenefitType.EARLY_ACCESS)
                .map(EligibilityBenefit::getEarlyAccessAt)
                .filter(value -> value != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        if (earliestAccess != null && at.isBefore(earliestAccess)) {
            throw new BusinessRuleException("El acceso anticipado para tu grupo todavía no ha comenzado.");
        }
    }
}
