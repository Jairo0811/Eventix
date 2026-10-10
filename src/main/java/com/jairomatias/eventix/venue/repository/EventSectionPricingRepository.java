package com.jairomatias.eventix.venue.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.venue.entity.EventSectionPricing;

public interface EventSectionPricingRepository
        extends JpaRepository<EventSectionPricing, Long> {

    @EntityGraph(attributePaths = {"section", "section.venue", "ticketType"})
    List<EventSectionPricing>
            findAllByEvent_IdOrderBySection_SortOrderAscTicketType_NameAsc(Long eventId);

    @EntityGraph(attributePaths = {"section", "section.venue", "ticketType"})
    Optional<EventSectionPricing> findByEvent_IdAndTicketType_Id(
            Long eventId,
            Long ticketTypeId);

    boolean existsByEvent_IdAndSection_Id(Long eventId, Long sectionId);

    boolean existsByEvent_IdAndSection_IdAndIdNot(
            Long eventId,
            Long sectionId,
            Long excludedId);

    boolean existsByEvent_IdAndTicketType_Id(Long eventId, Long ticketTypeId);

    boolean existsByEvent_IdAndTicketType_IdAndIdNot(
            Long eventId,
            Long ticketTypeId,
            Long excludedId);
}
