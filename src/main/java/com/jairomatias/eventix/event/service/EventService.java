package com.jairomatias.eventix.event.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.jairomatias.eventix.event.dto.EventDetailsView;
import com.jairomatias.eventix.event.dto.EventForm;
import com.jairomatias.eventix.event.dto.EventListItem;
import com.jairomatias.eventix.event.dto.OrganizerOption;
import com.jairomatias.eventix.event.entity.EventStatus;

public interface EventService {

    Page<EventListItem> findAll(
            String term,
            EventStatus status,
            Long categoryId,
            Long organizerId,
            String authenticatedLogin,
            Pageable pageable);

    EventDetailsView findById(
            Long id,
            String authenticatedLogin);

    EventForm getCreateForm(String authenticatedLogin);

    EventForm getUpdateForm(
            Long id,
            String authenticatedLogin);

    List<OrganizerOption> findEligibleOrganizers(
            String authenticatedLogin);

    Long create(
            EventForm form,
            String authenticatedLogin);

    void update(
            Long id,
            EventForm form,
            String authenticatedLogin);

    void delete(
            Long id,
            String authenticatedLogin);
}
