package com.jairomatias.eventix.promotion.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.promotion.entity.CouponRedemption;
import com.jairomatias.eventix.promotion.entity.CouponRedemptionStatus;

public interface CouponRedemptionRepository
        extends JpaRepository<CouponRedemption, Long> {

    long countByCoupon_IdAndBuyerEmailIgnoreCaseAndStatusIn(
            Long couponId,
            String buyerEmail,
            Collection<CouponRedemptionStatus> statuses);

    @EntityGraph(attributePaths = "coupon")
    Optional<CouponRedemption> findBySale_Id(Long saleId);
}
