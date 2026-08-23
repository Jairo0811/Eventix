package com.jairomatias.eventix.eligibility.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.EligibilityBenefitForm;
import com.jairomatias.eventix.eligibility.dto.EligibilityBenefitView;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefit;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.repository.EligibilityBenefitRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class EligibilityBenefitManagementService {

    private final EligibilityGroupRepository groupRepository;
    private final EligibilityBenefitRepository benefitRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;

    public EligibilityBenefitManagementService(
            EligibilityGroupRepository groupRepository,
            EligibilityBenefitRepository benefitRepository,
            TicketTypeRepository ticketTypeRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.benefitRepository = benefitRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EligibilityBenefitView> list(Long groupId, Long actorId) {
        EligibilityGroup group = getGroup(groupId);
        authorize(actorId, group.getEvent());
        return benefitRepository.findAllByGroup_IdOrderByBenefitTypeAsc(groupId)
                .stream()
                .map(EligibilityBenefitView::from)
                .toList();
    }

    @Transactional
    public Long create(Long groupId, EligibilityBenefitForm form, Long actorId) {
        EligibilityGroup group = getGroupForUpdate(groupId);
        authorize(actorId, group.getEvent());
        if (!group.isActive()) {
            throw new BusinessRuleException("No puedes agregar beneficios a un grupo inactivo.");
        }
        validate(form);
        TicketType ticketType = resolveTicketType(group.getEvent().getId(), form.ticketTypeId());

        EligibilityBenefit benefit = new EligibilityBenefit(
                group,
                form.benefitType(),
                normalizedDiscount(form),
                normalizedPurchaseLimit(form),
                normalizedReservedInventory(form),
                ticketType,
                normalizedEarlyAccess(form));
        return benefitRepository.save(benefit).getId();
    }

    @Transactional
    public void setActive(Long benefitId, boolean active, Long actorId) {
        EligibilityBenefit benefit = benefitRepository.findById(benefitId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el beneficio."));
        authorize(actorId, benefit.getGroup().getEvent());
        if (active) {
            benefit.activate();
        } else {
            benefit.deactivate();
        }
        benefitRepository.save(benefit);
    }

    private EligibilityGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo de elegibilidad."));
    }

    private EligibilityGroup getGroupForUpdate(Long groupId) {
        return groupRepository.findDetailedByIdForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el grupo de elegibilidad."));
    }

    private TicketType resolveTicketType(Long eventId, Long ticketTypeId) {
        if (ticketTypeId == null) {
            return null;
        }
        TicketType ticketType = ticketTypeRepository.findDetailedById(ticketTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de entrada."));
        if (!ticketType.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleException("El tipo de entrada no pertenece al evento del grupo.");
        }
        return ticketType;
    }

    private void validate(EligibilityBenefitForm form) {
        if (form == null || form.benefitType() == null) {
            throw new BusinessRuleException("El tipo de beneficio es obligatorio.");
        }
        EligibilityBenefitType type = form.benefitType();
        switch (type) {
            case PERCENTAGE_DISCOUNT -> {
                requirePositiveDiscount(form.discountValue());
                if (form.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessRuleException("El descuento porcentual no puede superar 100%.");
                }
            }
            case FIXED_DISCOUNT -> requirePositiveDiscount(form.discountValue());
            case PURCHASE_LIMIT -> {
                if (form.maxTicketsPerPurchase() == null || form.maxTicketsPerPurchase() < 1) {
                    throw new BusinessRuleException("Debes indicar un límite de compra mayor que cero.");
                }
            }
            case RESERVED_INVENTORY -> {
                if (form.reservedInventory() == null || form.reservedInventory() < 1) {
                    throw new BusinessRuleException("Debes indicar un inventario reservado mayor que cero.");
                }
            }
            case EXCLUSIVE_TICKET -> {
                if (form.ticketTypeId() == null) {
                    throw new BusinessRuleException("La entrada exclusiva debe indicar un tipo de entrada.");
                }
            }
            case EARLY_ACCESS -> {
                if (form.earlyAccessAt() == null) {
                    throw new BusinessRuleException("El acceso anticipado debe indicar fecha y hora.");
                }
            }
            case FREE_ENTRY, PRIORITY_ACCESS -> {
                // No additional scalar configuration is required.
            }
            default -> throw new BusinessRuleException("El tipo de beneficio no está soportado.");
        }
    }

    private void requirePositiveDiscount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Debes indicar un valor de descuento mayor que cero.");
        }
    }

    private BigDecimal normalizedDiscount(EligibilityBenefitForm form) {
        return switch (form.benefitType()) {
            case PERCENTAGE_DISCOUNT, FIXED_DISCOUNT -> form.discountValue();
            default -> null;
        };
    }

    private Integer normalizedPurchaseLimit(EligibilityBenefitForm form) {
        return form.benefitType() == EligibilityBenefitType.PURCHASE_LIMIT
                ? form.maxTicketsPerPurchase()
                : null;
    }

    private Integer normalizedReservedInventory(EligibilityBenefitForm form) {
        return form.benefitType() == EligibilityBenefitType.RESERVED_INVENTORY
                ? form.reservedInventory()
                : null;
    }

    private java.time.LocalDateTime normalizedEarlyAccess(EligibilityBenefitForm form) {
        return form.benefitType() == EligibilityBenefitType.EARLY_ACCESS
                ? form.earlyAccessAt()
                : null;
    }

    private void authorize(Long actorId, Event event) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario autenticado."));
        RoleName role = actor.getRole().getName();
        if (role == RoleName.ADMINISTRATOR) {
            return;
        }
        if (role == RoleName.ORGANIZER && event.getOrganizer().getId().equals(actorId)) {
            return;
        }
        throw new BusinessRuleException("No tienes permisos para administrar la elegibilidad de este evento.");
    }
}
