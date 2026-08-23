package com.jairomatias.eventix.notification.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.jairomatias.eventix.notification.entity.InternalNotificationType;
import com.jairomatias.eventix.notification.service.InternalNotificationService;
import com.jairomatias.eventix.sale.event.SalePaidEvent;
import com.jairomatias.eventix.sale.event.SaleRefundedEvent;
import com.jairomatias.eventix.sale.repository.SaleRepository;

@Component
public class InternalNotificationEventListener {

    private final SaleRepository saleRepository;
    private final InternalNotificationService notificationService;

    public InternalNotificationEventListener(
            SaleRepository saleRepository,
            InternalNotificationService notificationService) {
        this.saleRepository = saleRepository;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSalePaid(SalePaidEvent event) {
        saleRepository.findDetailedById(event.saleId())
                .ifPresent(sale -> notificationService.notify(
                        sale.getEvent().getOrganizer(),
                        InternalNotificationType.PURCHASE,
                        "Nueva venta confirmada",
                        "La venta " + sale.getReferenceCode()
                                + " fue pagada correctamente.",
                        "/sales/" + sale.getId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleRefunded(SaleRefundedEvent event) {
        saleRepository.findDetailedById(event.saleId())
                .ifPresent(sale -> notificationService.notify(
                        sale.getEvent().getOrganizer(),
                        InternalNotificationType.REFUND,
                        "Reembolso procesado",
                        "La venta " + sale.getReferenceCode()
                                + " fue reembolsada.",
                        "/sales/" + sale.getId()));
    }
}
