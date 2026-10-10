package com.jairomatias.eventix.ticket.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        TicketingProperties properties = properties(
                objectMapper,
                keyPair,
                "https://oauth2.googleapis.com/token");
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
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((parts[0] + "." + parts[1])
                .getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(
                Base64.getUrlDecoder().decode(parts[2]))).isTrue();
        JsonNode claims = objectMapper.readTree(
                Base64.getUrlDecoder().decode(parts[1]));
        JsonNode pass = claims.path("payload")
                .path("eventTicketObjects").get(0);
        assertThat(pass.path("seatInfo")
                .path("seat")
                .path("defaultValue")
                .path("value").asText())
                .isEqualTo("A-12");
        assertThat(pass.path("barcode").path("value").asText())
                .isEqualTo("EVX1.TKT-ABC.AF-ABC.signature");
        assertThat(pass.path("ticketHolderName").asText())
                .isEqualTo("María Pérez");
        assertThat(claims.path("aud").asText()).isEqualTo("google");
        assertThat(claims.path("payload")
                .path("eventTicketClasses").get(0)
                .path("id").asText()).isEqualTo("123456789.event_8");
        assertThat(pass.path("ticketNumber").asText())
                .isEqualTo("TKT-ABC");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void synchronizeObtainsOauthTokenAndPatchesClassAndObject()
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA")
                .generateKeyPair();
        TicketingProperties properties = properties(
                objectMapper,
                keyPair,
                "https://oauth.example.test/token");
        TicketCryptographyService cryptography =
                mock(TicketCryptographyService.class);
        DigitalTicket ticket = ticket();
        when(cryptography.createQrPayload(ticket)).thenReturn(
                "EVX1.TKT-ABC.AF-ABC.signature");
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> oauth = mock(HttpResponse.class);
        HttpResponse<String> classPatch = mock(HttpResponse.class);
        HttpResponse<String> objectPatch = mock(HttpResponse.class);
        when(oauth.statusCode()).thenReturn(200);
        when(oauth.body()).thenReturn("{\"access_token\":\"access-123\"}");
        when(classPatch.statusCode()).thenReturn(200);
        when(objectPatch.statusCode()).thenReturn(200);
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(oauth, classPatch, objectPatch);
        DefaultGoogleWalletPassService service =
                new DefaultGoogleWalletPassService(
                        properties,
                        cryptography,
                        objectMapper,
                        httpClient,
                        Clock.fixed(
                                Instant.parse("2026-10-10T12:00:00Z"),
                                ZoneOffset.UTC));

        service.synchronize(ticket);

        ArgumentCaptor<HttpRequest> requests =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(3)).send(
                requests.capture(),
                any(HttpResponse.BodyHandler.class));
        List<HttpRequest> values = requests.getAllValues();
        assertThat(values.get(0).uri().toString())
                .isEqualTo("https://oauth.example.test/token");
        assertThat(values.get(0).method()).isEqualTo("POST");
        assertThat(values.get(1).uri().toString())
                .contains("eventticketclass/123456789.event_8");
        assertThat(values.get(1).method()).isEqualTo("PATCH");
        assertThat(values.get(1).headers().firstValue("Authorization"))
                .contains("Bearer access-123");
        assertThat(values.get(2).uri().toString())
                .contains("eventticketobject/123456789.ticket_tkt-abc");
        assertThat(values.get(2).method()).isEqualTo("PATCH");
        assertThat(values.get(2).headers().firstValue("Authorization"))
                .contains("Bearer access-123");
    }

    private TicketingProperties properties(
            ObjectMapper objectMapper,
            KeyPair keyPair,
            String tokenUri) throws Exception {
        String serviceAccount = objectMapper.writeValueAsString(Map.of(
                "client_email", "wallet@example.iam.gserviceaccount.com",
                "private_key", "-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getEncoder().encodeToString(
                                keyPair.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----",
                "token_uri", tokenUri));
        TicketingProperties properties = new TicketingProperties();
        properties.getGoogleWallet().setEnabled(true);
        properties.getGoogleWallet().setIssuerId("123456789");
        properties.getGoogleWallet().setServiceAccountJson(serviceAccount);
        properties.getGoogleWallet().setOrigins(
                List.of("https://eventix.example.com"));
        return properties;
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
        when(ticket.getSeat()).thenReturn("A-12");
        when(ticket.getStatus()).thenReturn(TicketStatus.ACTIVE);
        return ticket;
    }
}
