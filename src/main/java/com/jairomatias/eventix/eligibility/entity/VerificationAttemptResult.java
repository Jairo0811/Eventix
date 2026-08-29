package com.jairomatias.eventix.eligibility.entity;

public enum VerificationAttemptResult {
    VERIFIED,
    MANUAL_REVIEW,
    NO_MATCH,
    IDENTITY_NOT_FOUND,
    PROVIDER_UNAVAILABLE,
    REJECTED
}
