package com.jairomatias.eventix.eligibility.service;

import java.text.Normalizer;

import org.springframework.stereotype.Component;

@Component
public class PersonNameNormalizer {

    public String normalize(String value) {
        String result = value == null ? "" : value.trim().toUpperCase();
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = result.replaceAll("\\p{M}", "");
        result = result.replaceAll("[^A-Z0-9 ]", " ");
        return result.replaceAll("\\s+", " ").trim();
    }
}
