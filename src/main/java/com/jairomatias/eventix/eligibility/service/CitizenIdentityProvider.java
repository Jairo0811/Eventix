package com.jairomatias.eventix.eligibility.service;

public interface CitizenIdentityProvider {

    CitizenIdentityLookupResult lookupByNationalId(String nationalId);
}
