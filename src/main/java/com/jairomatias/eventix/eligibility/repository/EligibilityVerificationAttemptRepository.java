package com.jairomatias.eventix.eligibility.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.EligibilityVerificationAttempt;

public interface EligibilityVerificationAttemptRepository
        extends JpaRepository<EligibilityVerificationAttempt, Long> {
}
