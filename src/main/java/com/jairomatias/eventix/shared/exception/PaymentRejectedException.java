package com.jairomatias.eventix.shared.exception;

/**
 * Signals a payment gateway rejection that must remain visible to the caller
 * without rolling back the transaction record persisted for audit purposes.
 */
public class PaymentRejectedException extends BusinessRuleException {

    public PaymentRejectedException(String message) {
        super(message);
    }
}
