package com.jairomatias.eventix.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalIdentityRepository
        extends JpaRepository<ExternalIdentity, Long> {

    Optional<ExternalIdentity> findByProviderAndProviderSubject(
            String provider,
            String providerSubject);
}
