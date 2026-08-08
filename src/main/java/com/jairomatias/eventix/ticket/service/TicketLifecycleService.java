package com.jairomatias.eventix.ticket.service;

public interface TicketLifecycleService {

    void issueForPaidSale(Long saleId);

    void revokeForRefundedSale(Long saleId, String reason);

    int expireEndedTickets();
}
