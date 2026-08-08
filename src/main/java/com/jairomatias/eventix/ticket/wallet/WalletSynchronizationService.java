package com.jairomatias.eventix.ticket.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;

@Service
public class WalletSynchronizationService {

    private final DigitalTicketRepository ticketRepository;
    private final GoogleWalletPassService googleWalletService;
    private final AppleWalletPassService appleWalletService;

    public WalletSynchronizationService(
            DigitalTicketRepository ticketRepository,
            GoogleWalletPassService googleWalletService,
            AppleWalletPassService appleWalletService) {
        this.ticketRepository = ticketRepository;
        this.googleWalletService = googleWalletService;
        this.appleWalletService = appleWalletService;
    }

    @Transactional(readOnly = true)
    public void synchronize(Long ticketId) {
        DigitalTicket ticket = ticketRepository.findDetailedById(ticketId)
                .orElse(null);
        if (ticket == null) {
            return;
        }
        googleWalletService.synchronize(ticket);
        appleWalletService.notifyUpdate(ticket);
    }
}
