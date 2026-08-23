package com.jairomatias.eventix.eligibility.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.EligibilityDiscountDecision;
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

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

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
        Set<Long> groupIds = groupIds(memberships);

        enforceExclusiveTicket(groupIds, ticketTypeId);
        if (groupIds.isEmpty()) {
            return;
        }

        List<EligibilityBenefit> benefits = benefitRepository.findAllByGroup_IdInAndActiveTrue(groupIds);
        enforcePurchaseLimit(benefits, quantity);
    }

    @Transactional(readOnly = true)
    public Optional<EligibilityDiscountDecision> resolveMonetaryDiscount(
            Event event,
            User user,
            Long ticketTypeId,
            BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        Set<Long> groupIds = groupIds(verifiedMemberships(event, user));
        if (groupIds.isEmpty()) {
            return Optional.empty();
        }

        return benefitRepository.findAllByGroup_IdInAndActiveTrue(groupIds)
                .stream()
                .filter(this::isMonetaryBenefit)
                .filter(benefit -> appliesToTicket(benefit, ticketTypeId))
                .map(benefit -> toDiscountDecision(benefit, subtotal))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator
                        .comparing(EligibilityDiscountDecision::discountAmount)
                        .thenComparing(EligibilityDiscountDecision::benefitId));
    }

    private Optional<EligibilityDiscountDecision> toDiscountDecision(
            EligibilityBenefit benefit,
            BigDecimal subtotal) {
        BigDecimal discount = calculateDiscount(benefit, subtotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new EligibilityDiscountDecision(
                benefit.getId(),
                benefit.getBenefitType(),
                benefit.getDiscountValue(),
                discount));
    }

    private BigDecimal calculateDiscount(EligibilityBenefit benefit, BigDecimal subtotal) {
        BigDecimal normalizedSubtotal = subtotal.setScale(MONEY_SCALE, MONEY_ROUNDING);
        return switch (benefit.getBenefitType()) {
            case FREE_ENTRY -> normalizedSubtotal;
            case PERCENTAGE_DISCOUNT -> percentageDiscount(benefit.getDiscountValue(), normalizedSubtotal);
            case FIXED_DISCOUNT -> fixedDiscount(benefit.getDiscountValue(), normalizedSubtotal);
            default -> BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        };
    }

    private BigDecimal percentageDiscount(BigDecimal configuredValue, BigDecimal subtotal) {
        if (configuredValue == null
                || configuredValue.compareTo(BigDecimal.ZERO) <= 0
                || configuredValue.compareTo(ONE_HUNDRED) > 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }
        return subtotal.multiply(configuredValue)
                .divide(ONE_HUNDRED, MONEY_SCALE, MONEY_ROUNDING)
                .min(subtotal)
                .max(BigDecimal.ZERO)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private BigDecimal fixedDiscount(BigDecimal configuredValue, BigDecimal subtotal) {
        if (configuredValue == null || configuredValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }
        return configuredValue
                .setScale(MONEY_SCALE, MONEY_ROUNDING)
                .min(subtotal)
                .max(BigDecimal.ZERO)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private boolean isMonetaryBenefit(EligibilityBenefit benefit) {
        return benefit.getBenefitType() == EligibilityBenefitType.PERCENTAGE_DISCOUNT
                || benefit.getBenefitType() == EligibilityBenefitType.FIXED_DISCOUNT
                || benefit.getBenefitType() == EligibilityBenefitType.FREE_ENTRY;
    }

    private boolean appliesToTicket(EligibilityBenefit benefit, Long ticketTypeId) {
        return benefit.getTicketType() == null
                || benefit.getTicketType().getId().equals(ticketTypeId);
    }

    private Set<Long> groupIds(List<EligibilityMembership> memberships) {
        return memberships.stream()
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toSet());
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
