package com.jairomatias.eventix.ticket.wallet;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.jairomatias.eventix.ticket.event.TicketPassChangedEvent;

@Component
public class WalletSynchronizationListener {

    private final WalletSynchronizationService synchronizationService;

    public WalletSynchronizationListener(
            WalletSynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("walletTaskExecutor")
    public void onTicketPassChanged(TicketPassChangedEvent event) {
        synchronizationService.synchronize(event.ticketId());
    }
}
