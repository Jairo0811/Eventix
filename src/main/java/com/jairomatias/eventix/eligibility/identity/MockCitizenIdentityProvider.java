package com.jairomatias.eventix.eligibility.identity;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "test"})
public class MockCitizenIdentityProvider implements CitizenIdentityProvider {

    private static final Map<String, CitizenIdentity> IDENTITIES = Map.of(
            "00112345678", new CitizenIdentity("Ana Perez Gomez"),
            "00187654321", new CitizenIdentity("Luis Ramirez Santos"));

    @Override
    public Optional<CitizenIdentity> findByNationalId(String normalizedNationalId) {
        return Optional.ofNullable(IDENTITIES.get(normalizedNationalId));
    }
}
