package com.jairomatias.eventix.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.jairomatias.eventix.settlement.entity.SettlementStatus;

public record SettlementListItem(
        Long id,
        Long organizerId,
        String organizerName,
        LocalDate periodFrom,
        LocalDate periodTo,
        BigDecimal grossSales,
        BigDecimal discounts,
        BigDecimal refunds,
        BigDecimal platformCommission,
        BigDecimal organizerNet,
        SettlementStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt) {
}
