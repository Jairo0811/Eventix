package com.jairomatias.eventix.ticket.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

class DefaultGoogleWalletPassServiceTest {

    @Test
    void createsSignedSaveUrlWithEventClassAndTicketObject()
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA")
                .generateKeyPair();
        String serviceAccount = objectMapper.writeValueAsString(Map.of(
                "client_email", "wallet@example.iam.gserviceaccount.com",
                "private_key", "-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getEncoder().encodeToString(
                                keyPair.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----",
                "token_uri", "https://oauth2.googleapis.com/token"));
        TicketingProperties properties = new TicketingProperties();
        properties.getGoogleWallet().setEnabled(true);
        properties.getGoogleWallet().setIssuerId("123456789");
        properties.getGoogleWallet().setServiceAccountJson(serviceAccount);
        properties.getGoogleWallet().setOrigins(
                List.of("https://eventix.example.com"));
        TicketCryptographyService cryptography =
                mock(TicketCryptographyService.class);
        DigitalTicket ticket = ticket();
        when(cryptography.createQrPayload(ticket)).thenReturn(
                "EVX1.TKT-ABC.AF-ABC.signature");
        DefaultGoogleWalletPassService service =
                new DefaultGoogleWalletPassService(
                        properties,
                        cryptography,
                        objectMapper);

        String saveUrl = service.createSaveUrl(ticket);

        assertThat(saveUrl)
                .startsWith("https://pay.google.com/gp/v/save/");
        String jwt = saveUrl.substring(saveUrl.lastIndexOf('/') + 1);
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);
        JsonNode claims = objectMapper.readTree(
                Base64.getUrlDecoder().decode(parts[1]));
        assertThat(claims.path("aud").asText()).isEqualTo("google");
        assertThat(claims.path("payload")
                .path("eventTicketClasses").get(0)
                .path("id").asText()).isEqualTo("123456789.event_8");
        assertThat(claims.path("payload")
                .path("eventTicketObjects").get(0)
                .path("ticketNumber").asText())
                .isEqualTo("TKT-ABC");
    }

    private DigitalTicket ticket() {
        DigitalTicket ticket = mock(DigitalTicket.class);
        Event event = mock(Event.class);
        when(ticket.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(8L);
        when(event.getTitle()).thenReturn("Concierto Eventix");
        when(event.getStartAt()).thenReturn(
                LocalDateTime.of(2026, 8, 8, 19, 0));
        when(event.getEndAt()).thenReturn(
                LocalDateTime.of(2026, 8, 8, 23, 0));
        when(event.getVenue()).thenReturn("Teatro Nacional");
        when(event.getAddress()).thenReturn("Santo Domingo");
        when(ticket.getUniqueCode()).thenReturn("TKT-ABC");
        when(ticket.getAttendeeName()).thenReturn("María Pérez");
        when(ticket.getTicketTypeName()).thenReturn("VIP");
        when(ticket.getZone()).thenReturn("VIP");
        when(ticket.getStatus()).thenReturn(TicketStatus.ACTIVE);
        return ticket;
    }
}
