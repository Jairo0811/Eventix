package com.jairomatias.eventix.eligibility.dto;

public record SchoolEligibilityResult(
        boolean eligible,
        String status,
        String memberName,
        String promotionName,
        Integer graduationYear,
        String nationalIdLast4) {

    public static SchoolEligibilityResult verified(
            String memberName,
            String promotionName,
            int graduationYear,
            String nationalIdLast4) {
        return new SchoolEligibilityResult(
                true,
                "VERIFIED",
                memberName,
                promotionName,
                graduationYear,
                nationalIdLast4);
    }

    public static SchoolEligibilityResult manualReview(
            String memberName,
            String promotionName,
            int graduationYear,
            String nationalIdLast4) {
        return new SchoolEligibilityResult(
                false,
                "MANUAL_REVIEW",
                memberName,
                promotionName,
                graduationYear,
                nationalIdLast4);
    }

    public static SchoolEligibilityResult rejected(String status) {
        return new SchoolEligibilityResult(false, status, null, null, null, null);
    }

    public static SchoolEligibilityResult notFound() {
        return new SchoolEligibilityResult(false, "NOT_FOUND", null, null, null, null);
    }

    public static SchoolEligibilityResult identityNotFound() {
        return new SchoolEligibilityResult(false, "IDENTITY_NOT_FOUND", null, null, null, null);
    }

    public static SchoolEligibilityResult identityProviderUnavailable() {
        return new SchoolEligibilityResult(false, "IDENTITY_PROVIDER_UNAVAILABLE", null, null, null, null);
    }
}
