package com.jairomatias.eventix.reservation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jairomatias.eventix.reservation.dto.ReservationDetailsView;
import com.jairomatias.eventix.reservation.dto.ReservationForm;
import com.jairomatias.eventix.reservation.dto.ReservationListItem;
import com.jairomatias.eventix.reservation.entity.Reservation;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    ReservationListItem toListItem(Reservation reservation);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "eventStartAt", source = "event.startAt")
    @Mapping(target = "reservedByName", source = "reservedBy.fullName")
    ReservationDetailsView toDetailsView(Reservation reservation);

    @Mapping(target = "eventId", source = "event.id")
    ReservationForm toForm(Reservation reservation);
}
