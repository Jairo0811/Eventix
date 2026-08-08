package com.jairomatias.eventix.ticket.wallet;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;

public interface GoogleWalletPassService {

    boolean isAvailable();

    String createSaveUrl(DigitalTicket ticket);

    void synchronize(DigitalTicket ticket);
}
