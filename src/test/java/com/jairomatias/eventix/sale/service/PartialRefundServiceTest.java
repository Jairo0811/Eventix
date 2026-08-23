package com.jairomatias.eventix.sale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.payment.entity.PaymentProvider;
import com.jairomatias.eventix.payment.entity.PaymentStatus;
import com.jairomatias.eventix.payment.entity.PaymentTransaction;
import com.jairomatias.eventix.payment.entity.PaymentTransactionType;
import com.jairomatias.eventix.payment.gateway.PaymentGateway;
import com.jairomatias.eventix.payment.gateway.PaymentGatewayRegistry;
import com.jairomatias.eventix.payment.gateway.PaymentResult;
import com.jairomatias.eventix.payment.repository.PaymentTransactionRepository;
import com.jairomatias.eventix.reservation.entity.Reservation;
import com.jairomatias.eventix.sale.dto.PartialRefundForm;
import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

class PartialRefundServiceTest {

    private SaleRepository saleRepository;
    private DigitalTicketRepository ticketRepository;
    private PaymentTransactionRepository paymentRepository;
    private UserRepository userRepository;
    private PaymentGatewayRegistry gatewayRegistry;
    private TransactionReferenceGenerator referenceGenerator;
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private PartialRefundService service;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        ticketRepository = mock(DigitalTicketRepository.class);
        paymentRepository = mock(PaymentTransactionRepository.class);
        userRepository = mock(UserRepository.class);
        gatewayRegistry = mock(PaymentGatewayRegistry.class);
        referenceGenerator = mock(TransactionReferenceGenerator.class);
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        service = new PartialRefundService(
                saleRepository,
                ticketRepository,
                paymentRepository,
                userRepository,
                gatewayRegistry,
                referenceGenerator,
                eventPublisher);
    }

    @Test
    void refundsSelectedActiveTicketAndCompletesSaleWhenItIsTheLastOne() {
        Sale sale = mock(Sale.class);
        Event event = mock(Event.class);
        Reservation reservation = mock(Reservation.class);
        DigitalTicket ticket = mock(DigitalTicket.class);
        User actor = mock(User.class);
        PaymentTransaction charge = mock(PaymentTransaction.class);
        PaymentGateway gateway = mock(PaymentGateway.class);

        when(sale.getId()).thenReturn(10L);
        when(sale.getStatus()).thenReturn(SaleStatus.PAID);
        when(sale.getEvent()).thenReturn(event);
        when(sale.getReservation()).thenReturn(reservation);
        when(sale.getReferenceCode()).thenReturn("SAL-TEST");
        when(sale.getCurrency()).thenReturn("DOP");
        when(sale.getRemainingAmount()).thenReturn(new BigDecimal("500.00"));
        when(event.getStartAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(ticket.getStatus()).thenReturn(TicketStatus.ACTIVE);
        when(ticket.getId()).thenReturn(100L);
        when(saleRepository.findDetailedByIdForUpdate(10L)).thenReturn(Optional.of(sale));
        when(ticketRepository.findAllBySaleIdAndIdsForUpdate(10L, java.util.Set.of(100L)))
                .thenReturn(List.of(ticket));
        when(ticketRepository.findAllBySale_IdOrderBySequenceNumberAsc(10L))
                .thenReturn(List.of(ticket));
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(actor));
        when(paymentRepository.findFirstBySale_IdAndTransactionTypeAndStatusOrderByProcessedAtDesc(
                10L, PaymentTransactionType.CHARGE, PaymentStatus.APPROVED))
                .thenReturn(Optional.of(charge));
        when(charge.getProvider()).thenReturn(PaymentProvider.BANK_TRANSFER);
        when(gatewayRegistry.resolve(PaymentProvider.BANK_TRANSFER)).thenReturn(gateway);
        when(gateway.process(any())).thenReturn(new PaymentResult(
                PaymentStatus.APPROVED, "REF-1", "Aprobado"));
        when(referenceGenerator.generatePaymentReference()).thenReturn("PAY-REFUND");
        when(paymentRepository.existsByTransactionReference("PAY-REFUND")).thenReturn(false);

        PartialRefundForm form = new PartialRefundForm();
        form.setTicketIds(List.of(100L));
        form.setReason("Solicitud del comprador");

        BigDecimal amount = service.refundTickets(10L, form, "admin");

        assertThat(amount).isEqualByComparingTo("500.00");
        verify(paymentRepository).save(any(PaymentTransaction.class));
        verify(sale).recordRefund(
                new BigDecimal("500.00"),
                "Solicitud del comprador",
                any(LocalDateTime.class));
        verify(ticket).cancel(
                org.mockito.ArgumentMatchers.eq("Solicitud del comprador"),
                any(LocalDateTime.class));
        verify(reservation).cancel(
                org.mockito.ArgumentMatchers.eq("Solicitud del comprador"),
                any(LocalDateTime.class));
    }
}
