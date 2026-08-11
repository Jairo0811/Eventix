package com.jairomatias.eventix.notification.service;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailGateway emailGateway;

    public NotificationService(EmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    public void sendReservationConfirmation(String recipient, String reservationCode) {
        emailGateway.send(
                recipient,
                "Reserva confirmada | Eventix",
                "Tu reserva " + reservationCode + " ha sido confirmada correctamente."
        );
    }

    public void sendPurchaseConfirmation(String recipient, String saleCode) {
        emailGateway.send(
                recipient,
                "Compra confirmada | Eventix",
                "Tu compra " + saleCode + " ha sido procesada correctamente. Tu boleta está disponible en Eventix."
        );
    }

    public void sendCancellation(String recipient, String reference) {
        emailGateway.send(
                recipient,
                "Cancelación registrada | Eventix",
                "La operación " + reference + " ha sido cancelada."
        );
    }

    public void sendRefundConfirmation(String recipient, String reference) {
        emailGateway.send(
                recipient,
                "Reembolso procesado | Eventix",
                "El reembolso asociado a " + reference + " ha sido procesado."
        );
    }

    public void sendEventReminder(String recipient, String eventName) {
        emailGateway.send(
                recipient,
                "Tu evento se acerca | Eventix",
                "Recuerda que " + eventName + " se aproxima. Consulta tu boleta en Eventix antes de llegar."
        );
    }

    public void sendPasswordReset(String recipient, String resetUrl) {
        emailGateway.send(
                recipient,
                "Restablece tu contraseña | Eventix",
                "Recibimos una solicitud para restablecer tu contraseña. "
                        + "Usa este enlace dentro de los próximos 30 minutos: "
                        + resetUrl
                        + "\n\nSi no solicitaste este cambio, ignora este mensaje."
        );
    }
}
