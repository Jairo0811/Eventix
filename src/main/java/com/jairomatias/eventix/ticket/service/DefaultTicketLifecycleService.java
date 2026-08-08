package com.jairomatias.eventix.ticket.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.sale.entity.Sale;
import com.jairomatias.eventix.sale.entity.SaleItem;
import com.jairomatias.eventix.sale.entity.SaleStatus;
import com.jairomatias.eventix.sale.repository.SaleRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.event.TicketPassChangedEvent;
import com.jairomatias.eventix.ticket.repository.DigitalTicketRepository;
import com.jairomatias.eventix.ticket.security.SignedTicketPayload;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;
import com.jairomatias.eventix.ticket.security.TicketSigningPayload;

@Service
public class DefaultTicketLifecycleService
        implements TicketLifecycleService {

    private static final int MAX_CODE_ATTEMPTS = 8;

    private final SaleRepository saleRepository;
    private final DigitalTicketRepository ticketRepository;
    private final TicketCodeGenerator codeGenerator;
    private final TicketCryptographyService cryptographyService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public DefaultTicketLifecycleService(
            SaleRepository saleRepository,
            DigitalTicketRepository ticketRepository,
            TicketCodeGenerator codeGenerator,
            TicketCryptographyService cryptographyService,
            ApplicationEventPublisher eventPublisher) {
        this(
                saleRepository,
                ticketRepository,
                codeGenerator,
                cryptographyService,
                eventPublisher,
                Clock.systemDefaultZone());
    }

    DefaultTicketLifecycleService(
            SaleRepository saleRepository,
            DigitalTicketRepository ticketRepository,
            TicketCodeGenerator codeGenerator,
            TicketCryptographyService cryptographyService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.saleRepository = saleRepository;
        this.ticketRepository = ticketRepository;
        this.codeGenerator = codeGenerator;
        this.cryptographyService = cryptographyService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void issueForPaidSale(Long saleId) {
        saleRepository.flush();
        Sale sale = findSale(saleId);
        if (sale.getStatus() != SaleStatus.PAID) {
            throw new BusinessRuleException(
                    "Solo se emiten boletas para ventas pagadas.");
        }
        if (ticketRepository.existsBySale_Id(saleId)) {
            return;
        }

        LocalDateTime issuedAt = LocalDateTime.now(clock).withNano(0);
        List<DigitalTicket> tickets = new ArrayList<>();
        int sequence = 1;
        for (SaleItem item : sale.getItems()) {
            for (int unit = 0; unit < item.getQuantity(); unit++) {
                String uniqueCode = nextTicketCode();
                String antiFraudCode = nextAntiFraudCode();
                TicketSigningPayload payload = new TicketSigningPayload(
                        uniqueCode,
                        sale.getReferenceCode(),
                        sale.getEvent().getId(),
                        sale.getBuyerEmail(),
                        item.getTicketTypeName(),
                        sequence,
                        issuedAt,
                        antiFraudCode);
                SignedTicketPayload signed = cryptographyService.sign(payload);
                tickets.add(new DigitalTicket(
                        uniqueCode,
                        sale,
                        item,
                        sequence,
                        antiFraudCode,
                        signed.payloadHash(),
                        signed.signature(),
                        signed.keyId(),
                        issuedAt));
                sequence++;
            }
        }
        ticketRepository.saveAll(tickets);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeForRefundedSale(Long saleId, String reason) {
        List<DigitalTicket> tickets =
                ticketRepository.findAllBySale_IdOrderBySequenceNumberAsc(
                        saleId);
        LocalDateTime cancelledAt = LocalDateTime.now(clock);
        for (DigitalTicket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.ACTIVE
                    || ticket.getStatus() == TicketStatus.USED) {
                ticket.cancel(reason, cancelledAt);
                eventPublisher.publishEvent(
                        new TicketPassChangedEvent(ticket.getId()));
            }
        }
    }

    @Override
    @Transactional
    public int expireEndedTickets() {
        LocalDateTime expiredAt = LocalDateTime.now(clock).withNano(0);
        List<DigitalTicket> tickets = ticketRepository
                .findAllByStatusAndEvent_EndAtBefore(
                        TicketStatus.ACTIVE,
                        expiredAt);
        tickets.forEach(ticket -> {
            ticket.expire(expiredAt);
            eventPublisher.publishEvent(
                    new TicketPassChangedEvent(ticket.getId()));
        });
        return tickets.size();
    }

    private Sale findSale(Long id) {
        return saleRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la venta para emitir sus boletas."));
    }

    private String nextTicketCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String value = codeGenerator.generateTicketCode();
            if (!ticketRepository.existsByUniqueCode(value)) {
                return value;
            }
        }
        throw new BusinessRuleException(
                "No se pudo generar un código único de boleta.");
    }

    private String nextAntiFraudCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String value = codeGenerator.generateAntiFraudCode();
            if (!ticketRepository.existsByAntiFraudCode(value)) {
                return value;
            }
        }
        throw new BusinessRuleException(
                "No se pudo generar el código antifraude.");
    }
}
