package com.jairomatias.eventix.ticket.wallet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

class GoogleWalletSynchronizationFailureTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void oauthFailureStopsSynchronizationBeforePatchRequests()
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
                "token_uri", "https://oauth.example.test/token"));
        TicketingProperties properties = new TicketingProperties();
        properties.getGoogleWallet().setEnabled(true);
        properties.getGoogleWallet().setIssuerId("123456789");
        properties.getGoogleWallet().setServiceAccountJson(serviceAccount);
        properties.getGoogleWallet().setOrigins(
                List.of("https://eventix.example.com"));

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> oauthFailure = mock(HttpResponse.class);
        when(oauthFailure.statusCode()).thenReturn(503);
        when(oauthFailure.body()).thenReturn("unavailable");
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(oauthFailure);
        DigitalTicket ticket = mock(DigitalTicket.class);
        when(ticket.getUniqueCode()).thenReturn("TKT-FAIL");

        DefaultGoogleWalletPassService service =
                new DefaultGoogleWalletPassService(
                        properties,
                        mock(TicketCryptographyService.class),
                        objectMapper,
                        httpClient,
                        Clock.fixed(
                                Instant.parse("2026-10-10T12:00:00Z"),
                                ZoneOffset.UTC));

        service.synchronize(ticket);

        verify(httpClient, times(1)).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class));
    }
}
