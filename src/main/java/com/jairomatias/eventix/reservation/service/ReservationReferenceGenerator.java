package com.jairomatias.eventix.reservation.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class ReservationReferenceGenerator {

    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int RANDOM_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder reference = new StringBuilder("RES-");
        for (int index = 0; index < RANDOM_LENGTH; index++) {
            reference.append(ALPHABET[
                    secureRandom.nextInt(ALPHABET.length)]);
        }
        return reference.toString();
    }
}
