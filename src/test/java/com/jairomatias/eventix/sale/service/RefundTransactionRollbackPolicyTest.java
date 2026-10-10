package com.jairomatias.eventix.sale.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.sale.dto.PartialRefundForm;
import com.jairomatias.eventix.sale.dto.SaleActionForm;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.PaymentRejectedException;

class RefundTransactionRollbackPolicyTest {

    @Test
    void fullRefundMustCommitDeclinedPaymentAuditBeforePropagatingRejection()
            throws NoSuchMethodException {
        Method method = DefaultSaleService.class.getMethod(
                "refund",
                Long.class,
                SaleActionForm.class,
                String.class);

        assertPreservesRejectedPayment(method);
    }

    @Test
    void partialRefundMustCommitDeclinedPaymentAuditBeforePropagatingRejection()
            throws NoSuchMethodException {
        Method method = PartialRefundService.class.getMethod(
                "refundTickets",
                Long.class,
                PartialRefundForm.class,
                String.class);

        assertPreservesRejectedPayment(method);
    }

    @Test
    void paymentRejectedExceptionRemainsABusinessRuleException() {
        assertTrue(BusinessRuleException.class.isAssignableFrom(PaymentRejectedException.class));
    }

    private void assertPreservesRejectedPayment(Method method) {
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional, "Refund operations must define an explicit transaction policy");
        assertTrue(
                Arrays.asList(transactional.noRollbackFor()).contains(PaymentRejectedException.class),
                "Gateway rejections must not roll back their audit transaction");
    }
}
