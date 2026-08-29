package com.jairomatias.eventix.eligibility.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class PersonNameNormalizer {

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio.");
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s'-]", " ")
                .replace('-', ' ')
                .replace("'", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("El nombre completo no contiene caracteres válidos.");
        }
        return normalized;
    }
}
