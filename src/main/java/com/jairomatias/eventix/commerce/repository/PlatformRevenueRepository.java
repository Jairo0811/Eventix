package com.jairomatias.eventix.commerce.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.sale.entity.Sale;

public interface PlatformRevenueRepository extends Repository<Sale, Long> {

    @Query("""
            SELECT COALESCE(SUM(s.total), 0)
            FROM Sale s
            WHERE s.status = com.jairomatias.eventix.sale.entity.SaleStatus.PAID
            AND (:organizerId IS NULL OR s.event.organizer.id = :organizerId)
            """)
    BigDecimal sumGrossPaid(@Param("organizerId") Long organizerId);

    @Query("""
            SELECT COALESCE(SUM(s.platformFeeAmount), 0)
            FROM Sale s
            WHERE s.status = com.jairomatias.eventix.sale.entity.SaleStatus.PAID
            AND (:organizerId IS NULL OR s.event.organizer.id = :organizerId)
            """)
    BigDecimal sumPlatformRevenue(@Param("organizerId") Long organizerId);

    @Query("""
            SELECT COALESCE(SUM(s.organizerNetAmount), 0)
            FROM Sale s
            WHERE s.status = com.jairomatias.eventix.sale.entity.SaleStatus.PAID
            AND (:organizerId IS NULL OR s.event.organizer.id = :organizerId)
            """)
    BigDecimal sumOrganizerNet(@Param("organizerId") Long organizerId);

    @Query("""
            SELECT COALESCE(SUM(s.total), 0)
            FROM Sale s
            WHERE s.status = com.jairomatias.eventix.sale.entity.SaleStatus.REFUNDED
            AND (:organizerId IS NULL OR s.event.organizer.id = :organizerId)
            """)
    BigDecimal sumRefunded(@Param("organizerId") Long organizerId);
}
