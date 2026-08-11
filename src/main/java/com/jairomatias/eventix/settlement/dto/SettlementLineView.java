package com.jairomatias.eventix.settlement.dto;

import java.math.BigDecimal;

import com.jairomatias.eventix.settlement.entity.SettlementLineType;

public record SettlementLineView(
        Long saleId,
        String saleReference,
        String eventTitle,
        SettlementLineType lineType,
        BigDecimal grossAmount,
        BigDecimal discountAmount,
        BigDecimal refundAmount,
        BigDecimal platformCommission,
        BigDecimal organizerNet,
        boolean active) {
}
