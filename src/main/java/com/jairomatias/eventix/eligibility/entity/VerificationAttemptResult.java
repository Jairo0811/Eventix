package com.jairomatias.eventix.eligibility.entity;

public enum VerificationAttemptResult {
    VERIFIED,
    MANUAL_REVIEW,
    NO_MATCH,
    REJECTED,
    IDENTITY_NOT_FOUND,
    PROVIDER_UNAVAILABLE,
    AMBIGUOUS_MATCH
}
