package com.jairomatias.eventix.eligibility.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class ConfiguredCitizenIdentityProvider implements CitizenIdentityProvider {

    private final Map<String, String> identities;

    public ConfiguredCitizenIdentityProvider(
            @Value("${eventix.identity.dev-records:}") String configuredRecords) {
        this.identities = parse(configuredRecords);
    }

    @Override
    public CitizenIdentityLookupResult lookupByNationalId(String nationalId) {
        if (identities.isEmpty()) {
            return CitizenIdentityLookupResult.unavailable();
        }
        String fullName = identities.get(normalizeNationalId(nationalId));
        return fullName == null
                ? CitizenIdentityLookupResult.notFound()
                : CitizenIdentityLookupResult.found(fullName);
    }

    private Map<String, String> parse(String configuredRecords) {
        if (configuredRecords == null || configuredRecords.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (String item : configuredRecords.split(";")) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String[] parts = item.split("=", 2);
            if (parts.length != 2 || parts[1].isBlank()) {
                throw new IllegalArgumentException(
                        "EVENTIX_IDENTITY_DEV_RECORDS debe usar cedula=nombre separados por punto y coma.");
            }
            String nationalId = normalizeNationalId(parts[0]);
            if (nationalId.length() != 11) {
                throw new IllegalArgumentException(
                        "Cada cédula configurada en EVENTIX_IDENTITY_DEV_RECORDS debe contener 11 dígitos.");
            }
            result.put(nationalId, parts[1].trim());
        }
        return Map.copyOf(result);
    }

    private String normalizeNationalId(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
