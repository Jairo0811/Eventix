package com.jairomatias.eventix.sale.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class TransactionReferenceGenerator {

    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int RANDOM_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSaleReference() {
        return generate("SAL-");
    }

    public String generatePaymentReference() {
        return generate("PAY-");
    }

    private String generate(String prefix) {
        StringBuilder reference = new StringBuilder(prefix);
        for (int index = 0; index < RANDOM_LENGTH; index++) {
            reference.append(ALPHABET[
                    secureRandom.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }
}
