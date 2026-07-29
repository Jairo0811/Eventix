package com.jairomatias.eventix.user.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%*?";
    private static final String ALL = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS;
    private static final int LENGTH = 14;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        char[] password = new char[LENGTH];
        password[0] = randomCharacter(UPPERCASE);
        password[1] = randomCharacter(LOWERCASE);
        password[2] = randomCharacter(DIGITS);
        password[3] = randomCharacter(SYMBOLS);

        for (int index = 4; index < LENGTH; index++) {
            password[index] = randomCharacter(ALL);
        }

        shuffle(password);
        return new String(password);
    }

    private char randomCharacter(String source) {
        return source.charAt(secureRandom.nextInt(source.length()));
    }

    private void shuffle(char[] value) {
        for (int index = value.length - 1; index > 0; index--) {
            int randomIndex = secureRandom.nextInt(index + 1);
            char current = value[index];
            value[index] = value[randomIndex];
            value[randomIndex] = current;
        }
    }
}

