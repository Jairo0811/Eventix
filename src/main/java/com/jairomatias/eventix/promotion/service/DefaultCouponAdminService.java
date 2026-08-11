package com.jairomatias.eventix.promotion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.promotion.dto.CouponEventOption;
import com.jairomatias.eventix.promotion.dto.CouponForm;
import com.jairomatias.eventix.promotion.dto.CouponListItem;
import com.jairomatias.eventix.promotion.entity.Coupon;
import com.jairomatias.eventix.promotion.entity.DiscountType;
import com.jairomatias.eventix.promotion.repository.CouponRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;

@Service
public class DefaultCouponAdminService implements CouponAdminService {

    private static final int MONEY_SCALE = 2;

    private final CouponRepository couponRepository;
    private final EventRepository eventRepository;

    public DefaultCouponAdminService(
            CouponRepository couponRepository,
            EventRepository eventRepository) {
        this.couponRepository = couponRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<CouponListItem> findAll(
            String term,
            Boolean active,
            Pageable pageable) {
        String normalizedTerm = term == null ? "" : term.trim();
        return couponRepository.search(normalizedTerm, active, pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<CouponEventOption> findEventOptions() {
        return eventRepository.findAll(Sort.by("startAt").descending())
                .stream()
                .map(event -> new CouponEventOption(
                        event.getId(),
                        event.getTitle()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public CouponForm getForm(Long id) {
        Coupon coupon = findDetailed(id);
        CouponForm form = new CouponForm();
        form.setCode(coupon.getCode());
        form.setDescription(coupon.getDescription());
        form.setDiscountType(coupon.getDiscountType());
        form.setValue(coupon.getValue());
        form.setStartsAt(coupon.getStartsAt());
        form.setExpiresAt(coupon.getExpiresAt());
        form.setActive(coupon.isActive());
        form.setTotalUseLimit(coupon.getTotalUseLimit());
        form.setPerUserLimit(coupon.getPerUserLimit());
        form.setMinimumSubtotal(coupon.getMinimumSubtotal());
        form.setEventIds(coupon.getApplicableEvents().stream()
                .map(Event::getId)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new)));
        return form;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long create(CouponForm form) {
        validateRequiredFields(form);
        String code = normalizeCode(form.getCode());
        validateUniqueCode(code, null);
        validateRules(form, 0);
        Set<Event> events = resolveEvents(form.getEventIds());

        Coupon coupon = new Coupon(
                code,
                form.getDescription().trim(),
                form.getDiscountType(),
                money(form.getValue()),
                form.getStartsAt(),
                form.getExpiresAt(),
                form.isActive(),
                form.getTotalUseLimit(),
                form.getPerUserLimit(),
                nullableMoney(form.getMinimumSubtotal()),
                events);
        return couponRepository.save(coupon).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void update(Long id, CouponForm form) {
        validateRequiredFields(form);
        Coupon coupon = findDetailed(id);
        String code = normalizeCode(form.getCode());
        validateUniqueCode(code, id);
        validateRules(form, coupon.getCurrentUses());
        coupon.update(
                code,
                form.getDescription().trim(),
                form.getDiscountType(),
                money(form.getValue()),
                form.getStartsAt(),
                form.getExpiresAt(),
                form.isActive(),
                form.getTotalUseLimit(),
                form.getPerUserLimit(),
                nullableMoney(form.getMinimumSubtotal()),
                resolveEvents(form.getEventIds()));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void activate(Long id) {
        findDetailed(id).activate();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(Long id) {
        findDetailed(id).deactivate();
    }

    private void validateRules(CouponForm form, int currentUses) {
        if (form.getStartsAt() == null
                || form.getExpiresAt() == null
                || !form.getExpiresAt().isAfter(form.getStartsAt())) {
            throw new BusinessRuleException(
                    "La expiración debe ser posterior al inicio.");
        }
        if (form.getDiscountType() == DiscountType.PERCENTAGE
                && form.getValue() != null
                && form.getValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessRuleException(
                    "El descuento porcentual no puede exceder 100%.");
        }
        if (form.getValue().scale() > MONEY_SCALE
                || form.getValue().precision() - form.getValue().scale() > 10) {
            throw new BusinessRuleException(
                    "El valor admite hasta diez enteros y dos decimales.");
        }
        if (form.getTotalUseLimit() != null
                && form.getTotalUseLimit() < currentUses) {
            throw new BusinessRuleException(
                    "El límite total no puede ser menor que los usos actuales.");
        }
    }

    private void validateRequiredFields(CouponForm form) {
        if (form == null
                || form.getCode() == null
                || form.getCode().isBlank()
                || form.getDescription() == null
                || form.getDescription().isBlank()
                || form.getDiscountType() == null
                || form.getValue() == null
                || form.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException(
                    "Completa los datos obligatorios del cupón.");
        }
    }

    private Set<Event> resolveEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            throw new BusinessRuleException(
                    "Selecciona al menos un evento.");
        }
        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != new LinkedHashSet<>(eventIds).size()) {
            throw new BusinessRuleException(
                    "Uno de los eventos seleccionados no existe.");
        }
        return new LinkedHashSet<>(events);
    }

    private Coupon findDetailed(Long id) {
        return couponRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El cupón solicitado no existe."));
    }

    private void validateUniqueCode(String code, Long excludedId) {
        boolean duplicate = excludedId == null
                ? couponRepository.existsByCodeIgnoreCase(code)
                : couponRepository.existsByCodeIgnoreCaseAndIdNot(
                        code,
                        excludedId);
        if (duplicate) {
            throw new DuplicateResourceException(
                    "code",
                    "Ya existe un cupón con ese código.");
        }
    }

    private CouponListItem toListItem(Coupon coupon) {
        return new CouponListItem(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType(),
                coupon.getValue(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.isActive(),
                coupon.getTotalUseLimit(),
                coupon.getCurrentUses(),
                coupon.getPerUserLimit(),
                coupon.getMinimumSubtotal());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal nullableMoney(BigDecimal value) {
        return value == null ? null : money(value);
    }
}
