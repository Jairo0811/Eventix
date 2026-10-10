package com.jairomatias.eventix.venue.dto;

import java.util.List;

import com.jairomatias.eventix.venue.entity.VenueSectionType;

public record VenueLayoutView(
        Long id,
        String name,
        String address,
        String city,
        String countryCode,
        List<Section> sections) {

    public record Section(
            Long id,
            String code,
            String name,
            VenueSectionType type,
            int capacity,
            List<Row> rows) {
    }

    public record Row(
            Long id,
            String code,
            String label,
            List<Seat> seats) {
    }

    public record Seat(
            Long id,
            String seatNumber,
            String label,
            boolean accessible,
            boolean companionSeat) {
    }
}
