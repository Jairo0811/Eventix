package com.jairomatias.eventix.ticket.wallet;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;
import com.jairomatias.eventix.ticket.entity.TicketStatus;
import com.jairomatias.eventix.ticket.security.TicketCryptographyService;

@Service
public class DefaultGoogleWalletPassService
        implements GoogleWalletPassService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            DefaultGoogleWalletPassService.class);
    private static final String SAVE_URL = "https://pay.google.com/gp/v/save/";
    private static final String WALLET_SCOPE =
            "https://www.googleapis.com/auth/wallet_object.issuer";
    private static final ZoneId EVENTIX_ZONE =
            ZoneId.of("America/Santo_Domingo");

    private final TicketingProperties properties;
    private final TicketCryptographyService cryptographyService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Clock clock;

    @Autowired
    public DefaultGoogleWalletPassService(
            TicketingProperties properties,
            TicketCryptographyService cryptographyService,
            ObjectMapper objectMapper) {
        this(
                properties,
                cryptographyService,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build(),
                Clock.systemUTC());
    }

    DefaultGoogleWalletPassService(
            TicketingProperties properties,
            TicketCryptographyService cryptographyService,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            Clock clock) {
        this.properties = properties;
        this.cryptographyService = cryptographyService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    @Override
    public boolean isAvailable() {
        return properties.getGoogleWallet().configured();
    }

    @Override
    public String createSaveUrl(DigitalTicket ticket) {
        Credentials credentials = credentials();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", credentials.clientEmail());
        claims.put("aud", "google");
        claims.put("typ", "savetowallet");
        claims.put("iat", Instant.now(clock).getEpochSecond());
        claims.put("origins", properties.getGoogleWallet().getOrigins());
        claims.put("payload", Map.of(
                "eventTicketClasses", List.of(createClass(ticket)),
                "eventTicketObjects", List.of(createObject(ticket))));
        return SAVE_URL + signJwt(claims, credentials.privateKey());
    }

    @Override
    public void synchronize(DigitalTicket ticket) {
        if (!isAvailable()) {
            return;
        }
        try {
            Credentials credentials = credentials();
            String accessToken = accessToken(credentials);
            patch(
                    "eventticketclass/" + classId(ticket),
                    createClassPatch(ticket),
                    accessToken);
            patch(
                    "eventticketobject/" + objectId(ticket),
                    createObjectPatch(ticket),
                    accessToken);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "No se pudo sincronizar la boleta {} con Google Wallet: {}",
                    ticket.getUniqueCode(),
                    exception.getMessage());
        }
    }

    private Map<String, Object> createClass(DigitalTicket ticket) {
        Map<String, Object> value = new LinkedHashMap<>(
                createClassPatch(ticket));
        value.put("id", classId(ticket));
        value.put("issuerName", properties.getIssuerName());
        value.put("reviewStatus", "UNDER_REVIEW");
        return value;
    }

    private Map<String, Object> createClassPatch(DigitalTicket ticket) {
        return Map.of(
                "eventName", localized(ticket.getEvent().getTitle()),
                "dateTime", Map.of(
                        "start", offsetDate(ticket.getEvent().getStartAt()),
                        "end", offsetDate(ticket.getEvent().getEndAt())),
                "venue", Map.of(
                        "name", localized(ticket.getEvent().getVenue()),
                        "address", localized(ticket.getEvent().getAddress())),
                "reviewStatus", "UNDER_REVIEW");
    }

    private Map<String, Object> createObject(DigitalTicket ticket) {
        Map<String, Object> value = new LinkedHashMap<>(
                createObjectPatch(ticket));
        value.put("id", objectId(ticket));
        value.put("classId", classId(ticket));
        value.put("ticketHolderName", ticket.getAttendeeName());
        value.put("ticketNumber", ticket.getUniqueCode());
        value.put("hexBackgroundColor", "#0F7A52");
        return value;
    }

    private Map<String, Object> createObjectPatch(DigitalTicket ticket) {
        Map<String, Object> seatInfo = new LinkedHashMap<>();
        seatInfo.put("section", localized(
                ticket.getZone() == null
                        ? ticket.getTicketTypeName()
                        : ticket.getZone()));
        if (ticket.getSeat() != null) {
            seatInfo.put("seat", localized(ticket.getSeat()));
        }
        return Map.of(
                "state", googleState(ticket.getStatus()),
                "barcode", Map.of(
                        "type", "QR_CODE",
                        "value", cryptographyService.createQrPayload(ticket),
                        "alternateText", ticket.getUniqueCode()),
                "seatInfo", seatInfo);
    }

    private void patch(
            String resource,
            Map<String, Object> body,
            String accessToken) {
        try {
            String encodedResource = resource.substring(0,
                    resource.indexOf('/') + 1)
                    + URLEncoder.encode(
                            resource.substring(resource.indexOf('/') + 1),
                            StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://walletobjects.googleapis.com/walletobjects/v1/"
                            + encodedResource))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .method(
                            "PATCH",
                            HttpRequest.BodyPublishers.ofString(
                                    objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                LOGGER.debug(
                        "El pase {} todavía no fue guardado en Google Wallet.",
                        resource);
            } else if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Google Wallet respondió HTTP "
                        + response.statusCode());
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo serializar la actualización de Google Wallet.",
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "La sincronización con Google Wallet fue interrumpida.",
                    exception);
        }
    }

    private String accessToken(Credentials credentials) {
        long issuedAt = Instant.now(clock).getEpochSecond();
        Map<String, Object> assertion = Map.of(
                "iss", credentials.clientEmail(),
                "scope", WALLET_SCOPE,
                "aud", credentials.tokenUri(),
                "iat", issuedAt,
                "exp", issuedAt + 3600);
        String signedAssertion = signJwt(
                assertion,
                credentials.privateKey());
        String body = "grant_type="
                + URLEncoder.encode(
                        "urn:ietf:params:oauth:grant-type:jwt-bearer",
                        StandardCharsets.UTF_8)
                + "&assertion="
                + URLEncoder.encode(
                        signedAssertion,
                        StandardCharsets.UTF_8);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(credentials.tokenUri()))
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Google OAuth respondió HTTP "
                        + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            String token = json.path("access_token").asText();
            if (token.isBlank()) {
                throw new IllegalStateException(
                        "Google OAuth no devolvió un access token.");
            }
            return token;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo procesar la respuesta de Google OAuth.",
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "La autenticación de Google fue interrumpida.",
                    exception);
        }
    }

    private Credentials credentials() {
        if (!isAvailable()) {
            throw new BusinessRuleException(
                    "Google Wallet no está configurado.");
        }
        try {
            String jsonValue = decodePossiblyBase64(
                    properties.getGoogleWallet().getServiceAccountJson());
            JsonNode json = objectMapper.readTree(jsonValue);
            String clientEmail = required(json, "client_email");
            String privateKeyValue = required(json, "private_key");
            String tokenUri = json.path("token_uri").asText(
                    "https://oauth2.googleapis.com/token");
            return new Credentials(
                    clientEmail,
                    decodePrivateKey(privateKeyValue),
                    tokenUri);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "GOOGLE_WALLET_SERVICE_ACCOUNT_JSON no es JSON válido.",
                    exception);
        }
    }

    private String signJwt(
            Map<String, Object> claims,
            PrivateKey privateKey) {
        try {
            String header = base64Url(objectMapper.writeValueAsBytes(
                    Map.of("alg", "RS256", "typ", "JWT")));
            String payload = base64Url(
                    objectMapper.writeValueAsBytes(claims));
            String signingInput = header + "." + payload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + base64Url(signature.sign());
        } catch (GeneralSecurityException | JsonProcessingException exception) {
            throw new IllegalStateException(
                    "No se pudo firmar el JWT de Google Wallet.",
                    exception);
        }
    }

    private PrivateKey decodePrivateKey(String pem) {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(
                            Base64.getDecoder().decode(normalized)));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "La clave privada del Service Account no es válida.",
                    exception);
        }
    }

    private String classId(DigitalTicket ticket) {
        return properties.getGoogleWallet().getIssuerId()
                + ".event_"
                + ticket.getEvent().getId();
    }

    private String objectId(DigitalTicket ticket) {
        return properties.getGoogleWallet().getIssuerId()
                + ".ticket_"
                + ticket.getUniqueCode()
                        .replaceAll("[^A-Za-z0-9_-]", "_")
                        .toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> localized(String value) {
        return Map.of(
                "defaultValue",
                Map.of("language", "es-DO", "value", value));
    }

    private String offsetDate(java.time.LocalDateTime value) {
        return value.atZone(EVENTIX_ZONE)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String googleState(TicketStatus status) {
        return switch (status) {
            case ACTIVE -> "ACTIVE";
            case USED -> "COMPLETED";
            case EXPIRED -> "EXPIRED";
            case CANCELLED -> "INACTIVE";
        };
    }

    private String required(JsonNode json, String field) {
        String value = json.path(field).asText();
        if (value.isBlank()) {
            throw new IllegalStateException(
                    "Falta " + field + " en el Service Account.");
        }
        return value;
    }

    private String decodePossiblyBase64(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("base64:")) {
            return new String(
                    Base64.getDecoder().decode(
                            normalized.substring("base64:".length())),
                    StandardCharsets.UTF_8);
        }
        return normalized;
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }

    private record Credentials(
            String clientEmail,
            PrivateKey privateKey,
            String tokenUri) {
    }
}
