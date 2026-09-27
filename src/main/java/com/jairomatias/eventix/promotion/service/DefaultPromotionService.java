package com.jairomatias.eventix.promotion.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.promotion.entity.Coupon;
import com.jairomatias.eventix.promotion.entity.CouponRedemption;
import com.jairomatias.eventix.promotion.entity.CouponRedemptionStatus;
import com.jairomatias.eventix.promotion.entity.DiscountType;
import com.jairomatias.eventix.promotion.repository.CouponRedemptionRepository;
import com.jairomatias.eventix.promotion.repository.CouponRepository;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;

@Service
public class DefaultPromotionService implements PromotionService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final List<CouponRedemptionStatus> ACTIVE_USES = List.of(
            CouponRedemptionStatus.RESERVED,
            CouponRedemptionStatus.CONSUMED);

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    public DefaultPromotionService(
            CouponRepository couponRepository,
            CouponRedemptionRepository redemptionRepository) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public BigDecimal quoteDiscount(
            String couponCode,
            Sale sale,
            LocalDateTime at) {
        String normalizedCode = normalizeCode(couponCode);
        if (normalizedCode == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }

        Coupon coupon = couponRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new BusinessRuleException(
                        "El cupón indicado no existe."));
        validateCoupon(coupon, sale, at);
        BigDecimal discount = calculateDiscount(coupon, sale.getSubtotal());
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException(
                    "El cupón no genera descuento para esta venta.");
        }
        return discount;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveForSale(
            String couponCode,
            Sale sale,
            LocalDateTime at) {
        String normalizedCode = normalizeCode(couponCode);
        if (normalizedCode == null) {
            return;
        }

        Coupon coupon = couponRepository.findByCodeForUpdate(normalizedCode)
                .orElseThrow(() -> new BusinessRuleException(
                        "El cupón indicado no existe."));
        validateCoupon(coupon, sale, at);

        BigDecimal discount = calculateDiscount(coupon, sale.getSubtotal());
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException(
                    "El cupón no genera descuento para esta venta.");
        }
        sale.applyCoupon(coupon, discount);
        coupon.reserveUse();
        redemptionRepository.save(new CouponRedemption(
                coupon,
                sale,
                normalizeEmail(sale.getBuyerEmail()),
                discount,
                at));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeForSale(Long saleId, LocalDateTime at) {
        redemptionRepository.findBySale_Id(saleId)
                .ifPresent(redemption -> redemption.consume(at));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseForSale(Long saleId, LocalDateTime at) {
        redemptionRepository.findBySale_Id(saleId)
                .ifPresent(redemption -> {
                    if (redemption.release(at)) {
                        Coupon lockedCoupon = couponRepository
                                .findByCodeForUpdate(
                                        redemption.getCoupon().getCode())
                                .orElseThrow(() -> new BusinessRuleException(
                                        "No se encontró el cupón de la venta."));
                        lockedCoupon.releaseUse();
                    }
                });
    }

    private void validateCoupon(
            Coupon coupon,
            Sale sale,
            LocalDateTime at) {
        if (!coupon.isActive()) {
            throw new BusinessRuleException("El cupón está inactivo.");
        }
        if (at.isBefore(coupon.getStartsAt())) {
            throw new BusinessRuleException("El cupón todavía no está vigente.");
        }
        if (at.isAfter(coupon.getExpiresAt())) {
            throw new BusinessRuleException("El cupón ha expirado.");
        }
        if (!coupon.appliesTo(sale.getEvent().getId())) {
            throw new BusinessRuleException(
                    "El cupón no aplica al evento de esta venta.");
        }
        if (coupon.getMinimumSubtotal() != null
                && sale.getSubtotal().compareTo(
                        coupon.getMinimumSubtotal()) < 0) {
            throw new BusinessRuleException(
                    "La venta no alcanza el importe mínimo del cupón.");
        }
        if (coupon.getTotalUseLimit() != null
                && coupon.getCurrentUses() >= coupon.getTotalUseLimit()) {
            throw new BusinessRuleException(
                    "El cupón alcanzó su límite total de usos.");
        }
        validateBuyerLimit(coupon, sale.getBuyerEmail());
    }

    private void validateBuyerLimit(Coupon coupon, String buyerEmail) {
        if (coupon.getPerUserLimit() == null) {
            return;
        }
        long buyerUses = redemptionRepository
                .countByCoupon_IdAndBuyerEmailIgnoreCaseAndStatusIn(
                        coupon.getId(),
                        normalizeEmail(buyerEmail),
                        ACTIVE_USES);
        if (buyerUses >= coupon.getPerUserLimit()) {
            throw new BusinessRuleException(
                    "El comprador alcanzó el límite de usos del cupón.");
        }
    }

    private BigDecimal calculateDiscount(
            Coupon coupon,
            BigDecimal subtotal) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotal.multiply(coupon.getValue())
                    .divide(new BigDecimal("100"), MONEY_SCALE, MONEY_ROUNDING);
        } else {
            discount = coupon.getValue().setScale(
                    MONEY_SCALE,
                    MONEY_ROUNDING);
        }
        return discount.min(subtotal).max(BigDecimal.ZERO)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private String normalizeCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
