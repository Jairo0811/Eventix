package com.jairomatias.eventix.venue.dto;

public record VenueListItem(
        Long id,
        String name,
        String address,
        String city,
        String countryCode,
        String timeZone,
        boolean active,
        long seatCount) {
}
