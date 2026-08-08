package com.jairomatias.eventix.ticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleItem;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.security.SignedTicketPayload;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

@ExtendWith(MockitoExtension.class)
class DefaultTicketLifecycleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private DigitalTicketRepository ticketRepository;
    @Mock private TicketCodeGenerator codeGenerator;
    @Mock private TicketCryptographyService cryptographyService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Sale sale;
    @Mock private SaleItem saleItem;
    @Mock private Event event;

    private DefaultTicketLifecycleService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-08T22:30:00Z"),
                ZoneId.of("America/Santo_Domingo"));
        service = new DefaultTicketLifecycleService(
                saleRepository,
                ticketRepository,
                codeGenerator,
                cryptographyService,
                eventPublisher,
                clock);
    }

    @Test
    void issuesOneSignedTicketPerPaidUnit() {
        preparePaidSale();
        when(sale.getItems()).thenReturn(List.of(saleItem));
        when(sale.getReferenceCode()).thenReturn("SAL-ABCDEFGH2345");
        when(sale.getBuyerName()).thenReturn("María Pérez");
        when(sale.getBuyerEmail()).thenReturn("buyer@example.com");
        when(sale.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(8L);
        when(saleItem.getQuantity()).thenReturn(2);
        when(saleItem.getTicketTypeName()).thenReturn("VIP");
        when(codeGenerator.generateTicketCode())
                .thenReturn(
                        "TKT-ABCDEFGH23456789JKLM",
                        "TKT-NPQRSTUVWXYZ23456789");
        when(codeGenerator.generateAntiFraudCode())
                .thenReturn(
                        "AF-ABCDEFGH23456789JKLM",
                        "AF-NPQRSTUVWXYZ23456789");
        when(cryptographyService.sign(any()))
                .thenReturn(new SignedTicketPayload(
                        "a".repeat(64),
                        "signature",
                        "test-key"));

        service.issueForPaidSale(55L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DigitalTicket>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(ticketRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(DigitalTicket::getSequenceNumber)
                .containsExactly(1, 2);
        assertThat(captor.getValue())
                .extracting(DigitalTicket::getUniqueCode)
                .doesNotHaveDuplicates();
        assertThat(captor.getValue())
                .allSatisfy(ticket -> {
                    assertThat(ticket.getAttendeeEmail())
                            .isEqualTo("buyer@example.com");
                    assertThat(ticket.getSignatureKeyId())
                            .isEqualTo("test-key");
                });
    }

    @Test
    void repeatedPaidEventDoesNotIssueDuplicateTickets() {
        preparePaidSale();
        when(ticketRepository.existsBySale_Id(55L)).thenReturn(true);

        service.issueForPaidSale(55L);

        verify(ticketRepository, never()).saveAll(any());
        verify(cryptographyService, never()).sign(any());
    }

    private void preparePaidSale() {
        when(saleRepository.findDetailedById(55L))
                .thenReturn(Optional.of(sale));
        when(sale.getStatus()).thenReturn(SaleStatus.PAID);
    }
}
