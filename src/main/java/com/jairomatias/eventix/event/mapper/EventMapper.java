package com.jairomatias.eventix.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.jairomatias.eventix.event.dto.EventDetailsView;
import com.jairomatias.eventix.event.dto.EventForm;
import com.jairomatias.eventix.event.dto.EventListItem;
import com.jairomatias.eventix.event.entity.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "organizerName", source = "organizer.fullName")
    EventListItem toListItem(Event event);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "organizerId", source = "organizer.id")
    @Mapping(target = "organizerName", source = "organizer.fullName")
    EventDetailsView toDetailsView(Event event);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "organizerId", source = "organizer.id")
    @Mapping(target = "coverImage", source = "coverImageUrl")
    EventForm toForm(Event event);
}
