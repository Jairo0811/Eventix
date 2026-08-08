package com.jairomatias.eventix.ticket.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class TicketCodeGenerator {

    private static final char[] ALPHABET =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String generateTicketCode() {
        return "TKT-" + randomCharacters(20);
    }

    public String generateAntiFraudCode() {
        return "AF-" + randomCharacters(20);
    }

    private String randomCharacters(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            value.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return value.toString();
    }
}
