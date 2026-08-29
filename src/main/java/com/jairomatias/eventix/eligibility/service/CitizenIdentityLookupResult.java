package com.jairomatias.eventix.eligibility.service;

public record CitizenIdentityLookupResult(
        Status status,
        String fullName) {

    public enum Status {
        FOUND,
        NOT_FOUND,
        UNAVAILABLE
    }

    public static CitizenIdentityLookupResult found(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("El proveedor de identidad devolvió un nombre vacío.");
        }
        return new CitizenIdentityLookupResult(Status.FOUND, fullName.trim());
    }

    public static CitizenIdentityLookupResult notFound() {
        return new CitizenIdentityLookupResult(Status.NOT_FOUND, null);
    }

    public static CitizenIdentityLookupResult unavailable() {
        return new CitizenIdentityLookupResult(Status.UNAVAILABLE, null);
    }
}
