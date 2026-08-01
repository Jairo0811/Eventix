package com.jairomatias.eventix.sale.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.sale.entity.SaleItem;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM SaleItem i
            WHERE i.ticketType.id = :ticketTypeId
            AND i.sale.status IN (
                com.jairomatias.eventix.sale.entity.SaleStatus.PENDING,
                com.jairomatias.eventix.sale.entity.SaleStatus.PAID
            )
            """)
    long sumAllocatedQuantity(
            @Param("ticketTypeId") Long ticketTypeId);
}
