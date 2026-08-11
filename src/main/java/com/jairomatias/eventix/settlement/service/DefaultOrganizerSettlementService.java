package com.jairomatias.eventix.settlement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.settlement.dto.SettlementActionForm;
import com.jairomatias.eventix.settlement.dto.SettlementCreateForm;
import com.jairomatias.eventix.settlement.dto.SettlementDetailsView;
import com.jairomatias.eventix.settlement.dto.SettlementLineView;
import com.jairomatias.eventix.settlement.dto.SettlementListItem;
import com.jairomatias.eventix.settlement.dto.SettlementOrganizerOption;
import com.jairomatias.eventix.settlement.entity.OrganizerSettlement;
import com.jairomatias.eventix.settlement.entity.OrganizerSettlementLine;
import com.jairomatias.eventix.settlement.entity.SettlementStatus;
import com.jairomatias.eventix.settlement.repository.OrganizerSettlementRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultOrganizerSettlementService
        implements OrganizerSettlementService {

    private static final long MAX_PERIOD_DAYS = 366L * 5L;
    private static final int MAX_REFERENCE_LENGTH = 120;
    private static final int MAX_NOTES_LENGTH = 1000;
    private static final List<SaleStatus> SETTLEABLE_SALE_STATUSES = List.of(
            SaleStatus.PAID,
            SaleStatus.REFUNDED);

    private final OrganizerSettlementRepository settlementRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public DefaultOrganizerSettlementService(
            OrganizerSettlementRepository settlementRepository,
            SaleRepository saleRepository,
            UserRepository userRepository) {
        this(
                settlementRepository,
                saleRepository,
                userRepository,
                Clock.systemDefaultZone());
    }

    DefaultOrganizerSettlementService(
            OrganizerSettlementRepository settlementRepository,
            SaleRepository saleRepository,
            UserRepository userRepository,
            Clock clock) {
        this.settlementRepository = settlementRepository;
        this.saleRepository = saleRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public Page<SettlementListItem> findAll(
            SettlementStatus status,
            String authenticatedLogin,
            Pageable pageable) {
        User actor = findActor(authenticatedLogin);
        Long organizerId = actor.getRole().getName() == RoleName.ORGANIZER
                ? actor.getId()
                : null;
        return settlementRepository.search(organizerId, status, pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public SettlementDetailsView findById(
            Long id,
            String authenticatedLogin) {
        User actor = findActor(authenticatedLogin);
        OrganizerSettlement settlement = findDetailed(id);
        ensureCanView(settlement, actor);
        return toDetailsView(settlement);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public SettlementCreateForm getCreateForm() {
        LocalDate previousMonth = today().minusMonths(1);
        SettlementCreateForm form = new SettlementCreateForm();
        form.setPeriodFrom(previousMonth.withDayOfMonth(1));
        form.setPeriodTo(previousMonth.withDayOfMonth(
                previousMonth.lengthOfMonth()));
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<SettlementOrganizerOption> findOrganizerOptions() {
        return userRepository
                .findAllByRole_NameAndStatusOrderByLastNameAscFirstNameAsc(
                        RoleName.ORGANIZER,
                        UserStatus.ACTIVE)
                .stream()
                .map(user -> new SettlementOrganizerOption(
                        user.getId(),
                        user.getFullName()))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long create(SettlementCreateForm form) {
        validateCreateForm(form);
        User organizer = findOrganizer(form.getOrganizerId());
        LocalDateTime fromDate = form.getPeriodFrom().atStartOfDay();
        LocalDateTime toDate = form.getPeriodTo().plusDays(1).atStartOfDay();

        List<Sale> sales = saleRepository.findUnsettledSalesForUpdate(
                organizer.getId(),
                SETTLEABLE_SALE_STATUSES,
                fromDate,
                toDate);
        List<Sale> refunds = saleRepository.findUnsettledRefundsForUpdate(
                organizer.getId(),
                fromDate,
                toDate);
        if (sales.isEmpty() && refunds.isEmpty()) {
            throw new BusinessRuleException(
                    "No hay ventas o reembolsos pendientes de liquidar en el período.");
        }

        OrganizerSettlement settlement = new OrganizerSettlement(
                organizer,
                form.getPeriodFrom(),
                form.getPeriodTo(),
                normalizeLimited(
                        form.getAdministrativeNotes(),
                        MAX_NOTES_LENGTH,
                        "Las observaciones no pueden superar 1000 caracteres."));
        sales.forEach(settlement::addSale);
        refunds.forEach(settlement::addRefund);
        return settlementRepository.save(settlement).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void startProcessing(Long id, SettlementActionForm form) {
        applyTransition(() -> findForUpdate(id).startProcessing(
                now(),
                normalizedNotes(form)));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void markPaid(Long id, SettlementActionForm form) {
        applyTransition(() -> findForUpdate(id).markPaid(
                now(),
                normalizeLimited(
                        form == null ? null : form.getExternalReference(),
                        MAX_REFERENCE_LENGTH,
                        "La referencia no puede superar 120 caracteres."),
                normalizedNotes(form)));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void markFailed(Long id, SettlementActionForm form) {
        String notes = requiredNotes(form, "Indica el motivo del fallo.");
        applyTransition(() -> findForUpdate(id).markFailed(now(), notes));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void cancel(Long id, SettlementActionForm form) {
        String notes = requiredNotes(form, "Indica el motivo de cancelación.");
        applyTransition(() -> findForUpdate(id).cancel(notes));
    }

    private void validateCreateForm(SettlementCreateForm form) {
        if (form == null
                || form.getOrganizerId() == null
                || form.getPeriodFrom() == null
                || form.getPeriodTo() == null) {
            throw new BusinessRuleException(
                    "Completa el organizador y el período.");
        }
        if (form.getPeriodFrom().isAfter(form.getPeriodTo())) {
            throw new BusinessRuleException(
                    "El inicio no puede ser posterior al final del período.");
        }
        if (form.getPeriodTo().isAfter(today())) {
            throw new BusinessRuleException(
                    "No se pueden liquidar períodos futuros.");
        }
        if (ChronoUnit.DAYS.between(
                form.getPeriodFrom(),
                form.getPeriodTo()) > MAX_PERIOD_DAYS) {
            throw new BusinessRuleException(
                    "El período de liquidación no puede superar cinco años.");
        }
    }

    private User findOrganizer(Long id) {
        User organizer = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el organizador."));
        if (organizer.getRole().getName() != RoleName.ORGANIZER
                || organizer.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "La liquidación requiere un organizador activo.");
        }
        return organizer;
    }

    private User findActor(String login) {
        return userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario autenticado."));
    }

    private OrganizerSettlement findDetailed(Long id) {
        return settlementRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la liquidación."));
    }

    private OrganizerSettlement findForUpdate(Long id) {
        return settlementRepository.findDetailedByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la liquidación."));
    }

    private void ensureCanView(
            OrganizerSettlement settlement,
            User actor) {
        if (actor.getRole().getName() == RoleName.ADMINISTRATOR) {
            return;
        }
        if (actor.getRole().getName() == RoleName.ORGANIZER
                && settlement.getOrganizer().getId().equals(actor.getId())) {
            return;
        }
        throw new BusinessRuleException(
                "No tienes permiso para consultar esta liquidación.");
    }

    private void applyTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new BusinessRuleException(exception.getMessage());
        }
    }

    private String requiredNotes(
            SettlementActionForm form,
            String message) {
        String notes = normalizedNotes(form);
        if (notes == null) {
            throw new BusinessRuleException(message);
        }
        return notes;
    }

    private String normalizedNotes(SettlementActionForm form) {
        return normalizeLimited(
                form == null ? null : form.getAdministrativeNotes(),
                MAX_NOTES_LENGTH,
                "Las observaciones no pueden superar 1000 caracteres.");
    }

    private String normalizeLimited(
            String value,
            int maximumLength,
            String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new BusinessRuleException(message);
        }
        return normalized;
    }

    private SettlementListItem toListItem(OrganizerSettlement settlement) {
        return new SettlementListItem(
                settlement.getId(),
                settlement.getOrganizer().getId(),
                settlement.getOrganizer().getFullName(),
                settlement.getPeriodFrom(),
                settlement.getPeriodTo(),
                settlement.getGrossSales(),
                settlement.getDiscounts(),
                settlement.getRefunds(),
                settlement.getPlatformCommission(),
                settlement.getOrganizerNet(),
                settlement.getStatus(),
                settlement.getCreatedAt(),
                settlement.getPaidAt());
    }

    private SettlementDetailsView toDetailsView(
            OrganizerSettlement settlement) {
        List<SettlementLineView> lines = settlement.getLines().stream()
                .sorted(Comparator
                        .comparing((OrganizerSettlementLine line) ->
                                line.getSale().getReferenceCode())
                        .thenComparing(OrganizerSettlementLine::getLineType))
                .map(this::toLineView)
                .toList();
        return new SettlementDetailsView(
                settlement.getId(),
                settlement.getOrganizer().getId(),
                settlement.getOrganizer().getFullName(),
                settlement.getPeriodFrom(),
                settlement.getPeriodTo(),
                settlement.getGrossSales(),
                settlement.getDiscounts(),
                settlement.getRefunds(),
                settlement.getPlatformCommission(),
                settlement.getOrganizerNet(),
                settlement.getStatus(),
                settlement.getCreatedAt(),
                settlement.getProcessedAt(),
                settlement.getPaidAt(),
                settlement.getExternalReference(),
                settlement.getAdministrativeNotes(),
                lines);
    }

    private SettlementLineView toLineView(OrganizerSettlementLine line) {
        return new SettlementLineView(
                line.getSale().getId(),
                line.getSale().getReferenceCode(),
                line.getSale().getEvent().getTitle(),
                line.getLineType(),
                line.getGrossAmount(),
                line.getDiscountAmount(),
                line.getRefundAmount(),
                line.getPlatformCommission(),
                line.getOrganizerNet(),
                line.isActive());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
