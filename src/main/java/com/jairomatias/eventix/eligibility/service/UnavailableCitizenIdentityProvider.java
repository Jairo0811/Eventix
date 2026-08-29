package com.jairomatias.eventix.eligibility.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!dev")
public class UnavailableCitizenIdentityProvider implements CitizenIdentityProvider {

    @Override
    public CitizenIdentityLookupResult lookupByNationalId(String nationalId) {
        return CitizenIdentityLookupResult.unavailable();
    }
}
