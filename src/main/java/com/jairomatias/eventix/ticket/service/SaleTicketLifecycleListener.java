package com.jairomatias.eventix.ticket.service;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.event.SaleRefundedEvent;

@Component
public class SaleTicketLifecycleListener {

    private final TicketLifecycleService lifecycleService;

    public SaleTicketLifecycleListener(
            TicketLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @EventListener
    @Order(100)
    public void onSalePaid(SalePaidEvent event) {
        lifecycleService.issueForPaidSale(event.saleId());
    }

    @EventListener
    @Order(100)
    public void onSaleRefunded(SaleRefundedEvent event) {
        lifecycleService.revokeForRefundedSale(
                event.saleId(),
                event.reason());
    }
}
