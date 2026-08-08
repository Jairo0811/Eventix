package com.jairomatias.eventix.ticket.wallet;

import com.jairomatias.eventix.ticket.entity.DigitalTicket;

public interface AppleWalletPassService {

    boolean isAvailable();

    byte[] createPass(DigitalTicket ticket);

    void notifyUpdate(DigitalTicket ticket);
}
