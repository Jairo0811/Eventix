package com.jairomatias.eventix.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    private EmailGateway emailGateway;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        emailGateway = mock(EmailGateway.class);
        notificationService = new NotificationService(emailGateway);
    }

    @Test
    void shouldSendReservationConfirmation() {
        notificationService.sendReservationConfirmation("user@example.com", "RSV-100");

        verify(emailGateway).send(
                "user@example.com",
                "Reserva confirmada | Eventix",
                "Tu reserva RSV-100 ha sido confirmada correctamente."
        );
    }

    @Test
    void shouldSendPurchaseConfirmation() {
        notificationService.sendPurchaseConfirmation("user@example.com", "SALE-100");

        verify(emailGateway).send(
                "user@example.com",
                "Compra confirmada | Eventix",
                "Tu compra SALE-100 ha sido procesada correctamente. Tu boleta está disponible en Eventix."
        );
    }

    @Test
    void shouldSendCancellation() {
        notificationService.sendCancellation("user@example.com", "RSV-100");

        verify(emailGateway).send(
                "user@example.com",
                "Cancelación registrada | Eventix",
                "La operación RSV-100 ha sido cancelada."
        );
    }

    @Test
    void shouldSendRefundConfirmation() {
        notificationService.sendRefundConfirmation("user@example.com", "SALE-100");

        verify(emailGateway).send(
                "user@example.com",
                "Reembolso procesado | Eventix",
                "El reembolso asociado a SALE-100 ha sido procesado."
        );
    }

    @Test
    void shouldSendEventReminder() {
        notificationService.sendEventReminder("user@example.com", "Eventix Live");

        verify(emailGateway).send(
                "user@example.com",
                "Tu evento se acerca | Eventix",
                "Recuerda que Eventix Live se aproxima. Consulta tu boleta en Eventix antes de llegar."
        );
    }
}
