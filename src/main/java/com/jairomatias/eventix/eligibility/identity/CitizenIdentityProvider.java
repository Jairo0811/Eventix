package com.jairomatias.eventix.eligibility.identity;

import java.util.Optional;

public interface CitizenIdentityProvider {

    Optional<CitizenIdentity> findByNationalId(String normalizedNationalId);
}
