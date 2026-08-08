package com.jairomatias.eventix.ticket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.repository.SaleRepository;

@Component
public class TicketExpirationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            TicketExpirationScheduler.class);

    private final TicketLifecycleService lifecycleService;
    private final SaleRepository saleRepository;

    public TicketExpirationScheduler(
            TicketLifecycleService lifecycleService,
            SaleRepository saleRepository) {
        this.lifecycleService = lifecycleService;
        this.saleRepository = saleRepository;
    }

    @Scheduled(
            fixedDelayString =
                    "${eventix.ticketing.expiration-scan-interval:PT5M}")
    public void expireEndedTickets() {
        int recoveredSales = 0;
        for (Long saleId : saleRepository
                .findSaleIdsWithoutTicketsByStatus(SaleStatus.PAID)) {
            lifecycleService.issueForPaidSale(saleId);
            recoveredSales++;
        }
        if (recoveredSales > 0) {
            LOGGER.info(
                    "Se recuperó la emisión de boletas para {} ventas pagadas.",
                    recoveredSales);
        }
        int expired = lifecycleService.expireEndedTickets();
        if (expired > 0) {
            LOGGER.info("Se vencieron {} boletas digitales.", expired);
        }
    }
}
