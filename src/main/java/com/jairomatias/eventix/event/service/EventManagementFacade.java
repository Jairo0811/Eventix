package com.jairomatias.eventix.event.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.eligibility.dto.SchoolAlumniBenefitConfiguration;
import com.jairomatias.eventix.eligibility.service.SchoolAlumniBenefitService;
import com.jairomatias.eventix.event.dto.EventForm;

@Service
public class EventManagementFacade {

    private final EventService eventService;
    private final EventLocationService eventLocationService;
    private final EventCoverImageStorage coverImageStorage;
    private final SchoolAlumniBenefitService schoolAlumniBenefitService;

    public EventManagementFacade(
            EventService eventService,
            EventLocationService eventLocationService,
            EventCoverImageStorage coverImageStorage,
            SchoolAlumniBenefitService schoolAlumniBenefitService) {
        this.eventService = eventService;
        this.eventLocationService = eventLocationService;
        this.coverImageStorage = coverImageStorage;
        this.schoolAlumniBenefitService = schoolAlumniBenefitService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public Long create(EventForm form, String authenticatedLogin) {
        String uploadedCover = coverImageStorage.store(form.getCoverImage());
        if (uploadedCover != null) {
            form.setCoverImageUrl(uploadedCover);
        }

        try {
            Long eventId = eventService.create(form, authenticatedLogin);
            eventLocationService.updateGoogleMapsUrl(
                    eventId,
                    form.getGoogleMapsUrl());
            configureSchoolAlumniBenefit(eventId, form, authenticatedLogin);
            return eventId;
        } catch (RuntimeException exception) {
            coverImageStorage.deleteManaged(uploadedCover);
            throw exception;
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void update(
            Long eventId,
            EventForm form,
            String authenticatedLogin) {
        String previousCover = form.getCoverImageUrl();
        String uploadedCover = coverImageStorage.store(form.getCoverImage());
        if (uploadedCover != null) {
            form.setCoverImageUrl(uploadedCover);
        }

        try {
            eventService.update(eventId, form, authenticatedLogin);
            eventLocationService.updateGoogleMapsUrl(
                    eventId,
                    form.getGoogleMapsUrl());
            configureSchoolAlumniBenefit(eventId, form, authenticatedLogin);
            if (uploadedCover != null) {
                coverImageStorage.deleteManaged(previousCover);
            }
        } catch (RuntimeException exception) {
            coverImageStorage.deleteManaged(uploadedCover);
            form.setCoverImageUrl(previousCover);
            throw exception;
        }
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZER')")
    public void enrichSchoolAlumniBenefitForm(
            Long eventId,
            EventForm form,
            String authenticatedLogin) {
        SchoolAlumniBenefitConfiguration configuration =
                schoolAlumniBenefitService.getConfiguration(
                        eventId,
                        authenticatedLogin);
        form.setSchoolAlumniBenefitEnabled(configuration.enabled());
        form.setSchoolPromotionId(configuration.schoolPromotionId());
        form.setSchoolAlumniDiscountType(configuration.discountType());
        form.setSchoolAlumniDiscountValue(configuration.discountValue());
    }

    private void configureSchoolAlumniBenefit(
            Long eventId,
            EventForm form,
            String authenticatedLogin) {
        schoolAlumniBenefitService.configure(
                eventId,
                Boolean.TRUE.equals(form.getSchoolAlumniBenefitEnabled()),
                form.getSchoolPromotionId(),
                form.getSchoolAlumniDiscountType(),
                form.getSchoolAlumniDiscountValue(),
                authenticatedLogin);
    }
}
