package com.jairomatias.eventix.promotion.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.sale.entity.Sale;

public interface PromotionService {

    BigDecimal quoteDiscount(String couponCode, Sale sale, LocalDateTime at);

    void reserveForSale(String couponCode, Sale sale, LocalDateTime at);

    void consumeForSale(Long saleId, LocalDateTime at);

    void releaseForSale(Long saleId, LocalDateTime at);
}
