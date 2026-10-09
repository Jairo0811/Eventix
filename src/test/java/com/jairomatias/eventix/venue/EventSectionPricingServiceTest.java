package com.jairomatias.eventix.venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.event.entity.EventSeatingMode;
import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.sale.entity.TicketType;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.repository.UserRepository;
import com.jairomatias.eventix.venue.dto.TicketSeatingRule;
import com.jairomatias.eventix.venue.entity.EventSectionPricing;
import com.jairomatias.eventix.venue.entity.Venue;
import com.jairomatias.eventix.venue.entity.VenueSection;
import com.jairomatias.eventix.venue.entity.VenueSectionType;
import com.jairomatias.eventix.venue.repository.EventSectionPricingRepository;
import com.jairomatias.eventix.venue.repository.VenueSectionRepository;
import com.jairomatias.eventix.venue.service.EventSectionPricingService;

@ExtendWith(MockitoExtension.class)
class EventSectionPricingServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueSectionRepository sectionRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private EventSectionPricingRepository pricingRepository;
    @Mock private UserRepository userRepository;
    @Mock private Event event;
    @Mock private TicketType ticketType;
    @Mock private EventSectionPricing pricing;
    @Mock private Venue venue;
    @Mock private VenueSection section;

    private EventSectionPricingService service;

    @BeforeEach
    void setUp() {
        service = new EventSectionPricingService(
                eventRepository,
                sectionRepository,
                ticketTypeRepository,
                pricingRepository,
                userRepository);
        org.mockito.Mockito.lenient().when(event.getId()).thenReturn(10L);
        org.mockito.Mockito.lenient().when(ticketType.getId()).thenReturn(31L);
        org.mockito.Mockito.lenient().when(ticketType.getName()).thenReturn("VIP");
        org.mockito.Mockito.lenient().when(ticketType.getPrice())
                .thenReturn(new BigDecimal("500.00"));
    }

    @Test
    void generalAdmissionNeverRequiresSectionMapping() {
        when(event.getSeatingMode()).thenReturn(EventSeatingMode.GENERAL_ADMISSION);

        TicketSeatingRule rule = service.resolveRule(event, ticketType);

        assertThat(rule.requiresSeatHold()).isFalse();
        assertThat(rule.sectionId()).isNull();
        assertThat(rule.unitPrice()).isEqualByComparingTo("500.00");
        verifyNoInteractions(pricingRepository);
    }

    @Test
    void mixedTicketWithoutMappingRemainsGeneralAdmission() {
        when(event.getSeatingMode()).thenReturn(EventSeatingMode.MIXED);
        when(pricingRepository.findByEvent_IdAndTicketType_Id(10L, 31L))
                .thenReturn(Optional.empty());

        TicketSeatingRule rule = service.resolveRule(event, ticketType);

        assertThat(rule.requiresSeatHold()).isFalse();
        assertThat(rule.sectionId()).isNull();
        assertThat(rule.unitPrice()).isEqualByComparingTo("500.00");
    }

    @Test
    void reservedEventRejectsUnmappedTicketType() {
        when(event.getSeatingMode()).thenReturn(EventSeatingMode.RESERVED_SEATING);
        when(pricingRepository.findByEvent_IdAndTicketType_Id(10L, 31L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveRule(event, ticketType))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no está asignado");
    }

    @Test
    void mixedReservedMappingRequiresHoldAndUsesOverridePrice() {
        when(event.getSeatingMode()).thenReturn(EventSeatingMode.MIXED);
        when(event.getVenueDefinition()).thenReturn(venue);
        when(venue.getId()).thenReturn(5L);
        when(pricingRepository.findByEvent_IdAndTicketType_Id(10L, 31L))
                .thenReturn(Optional.of(pricing));
        when(pricing.getSection()).thenReturn(section);
        when(section.getVenue()).thenReturn(venue);
        when(section.getId()).thenReturn(501L);
        when(section.isActive()).thenReturn(true);
        when(section.getSectionType()).thenReturn(VenueSectionType.RESERVED_SEATING);
        when(pricing.effectivePrice()).thenReturn(new BigDecimal("650.00"));

        TicketSeatingRule rule = service.resolveRule(event, ticketType);

        assertThat(rule.requiresSeatHold()).isTrue();
        assertThat(rule.sectionId()).isEqualTo(501L);
        assertThat(rule.unitPrice()).isEqualByComparingTo("650.00");
    }
}
