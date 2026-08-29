package com.jairomatias.eventix.eligibility.identity;

import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class UnavailableCitizenIdentityProvider implements CitizenIdentityProvider {

    @Override
    public Optional<CitizenIdentity> findByNationalId(String normalizedNationalId) {
        throw new CitizenIdentityProviderUnavailableException(
                "El proveedor oficial de identidad todavía no está configurado para producción.");
    }
}
