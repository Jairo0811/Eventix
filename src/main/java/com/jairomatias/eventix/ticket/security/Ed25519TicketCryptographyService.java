package com.jairomatias.eventix.ticket.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jairomatias.eventix.ticket.config.TicketingProperties;
import com.jairomatias.eventix.ticket.entity.DigitalTicket;

@Service
public class Ed25519TicketCryptographyService
        implements TicketCryptographyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            Ed25519TicketCryptographyService.class);
    private static final Base64.Encoder URL_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final PrivateKey privateKey;
    private final Map<String, PublicKey> verificationKeys;
    private final String keyId;

    public Ed25519TicketCryptographyService(
            TicketingProperties properties) {
        KeyPair keyPair = loadOrCreateKeyPair(properties);
        privateKey = keyPair.getPrivate();
        keyId = requireText(
                properties.getSigningKeyId(),
                "eventix.ticketing.signing-key-id");
        verificationKeys = loadVerificationKeys(
                properties,
                keyId,
                keyPair.getPublic());
    }

    @Override
    public SignedTicketPayload sign(TicketSigningPayload payload) {
        byte[] canonical = payload.canonicalValue()
                .getBytes(StandardCharsets.UTF_8);
        return new SignedTicketPayload(
                sha256(payload.canonicalValue()),
                URL_ENCODER.encodeToString(signBytes(canonical)),
                keyId);
    }

    @Override
    public boolean verify(
            DigitalTicket ticket,
            ParsedTicketToken submittedToken) {
        if (!ticket.getUniqueCode().equals(submittedToken.uniqueCode())
                || !ticket.getAntiFraudCode().equals(
                        submittedToken.antiFraudCode())
                || !ticket.getDigitalSignature().equals(
                        submittedToken.signature())) {
            return false;
        }

        PublicKey verificationKey = verificationKeys.get(
                ticket.getSignatureKeyId());
        if (verificationKey == null) {
            return false;
        }

        TicketSigningPayload payload = TicketSigningPayload.from(ticket);
        String expectedHash = sha256(payload.canonicalValue());
        if (!constantTimeEquals(
                expectedHash,
                ticket.getSignedPayloadHash())) {
            return false;
        }

        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(verificationKey);
            verifier.update(payload.canonicalValue()
                    .getBytes(StandardCharsets.UTF_8));
            return verifier.verify(URL_DECODER.decode(
                    submittedToken.signature()));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public String createQrPayload(DigitalTicket ticket) {
        return String.join(
                ".",
                "EVX1",
                ticket.getUniqueCode(),
                ticket.getAntiFraudCode(),
                ticket.getDigitalSignature());
    }

    @Override
    public Optional<ParsedTicketToken> parseQrPayload(String value) {
        if (value == null || value.length() > 512) {
            return Optional.empty();
        }
        String[] parts = value.trim().split("\\.", -1);
        if (parts.length != 4
                || !"EVX1".equals(parts[0])
                || parts[1].isBlank()
                || parts[2].isBlank()
                || parts[3].isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedTicketToken(
                parts[1],
                parts[2],
                parts[3]));
    }

    @Override
    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "SHA-256 no está disponible.",
                    exception);
        }
    }

    private byte[] signBytes(byte[] value) {
        try {
            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(privateKey);
            signer.update(value);
            return signer.sign();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "No se pudo firmar la boleta digital.",
                    exception);
        }
    }

    private KeyPair loadOrCreateKeyPair(TicketingProperties properties) {
        String privateValue = properties.getSigningPrivateKey();
        String publicValue = properties.getSigningPublicKey();
        boolean privateConfigured = hasText(privateValue);
        boolean publicConfigured = hasText(publicValue);

        if (privateConfigured != publicConfigured) {
            throw new IllegalStateException(
                    "Configura juntas TICKETING_SIGNING_PRIVATE_KEY y "
                    + "TICKETING_SIGNING_PUBLIC_KEY.");
        }
        if (privateConfigured) {
            return decodeKeyPair(privateValue, publicValue);
        }
        if (!properties.isAllowEphemeralSigningKey()) {
            throw new IllegalStateException(
                    "Las claves Ed25519 son obligatorias cuando la clave "
                    + "efímera está deshabilitada.");
        }
        try {
            LOGGER.warn(
                    "Eventix usa una clave Ed25519 efímera. Configura claves "
                    + "persistentes antes de producción.");
            return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "No se pudo crear la clave Ed25519.",
                    exception);
        }
    }

    private KeyPair decodeKeyPair(
            String privateValue,
            String publicValue) {
        try {
            KeyFactory factory = KeyFactory.getInstance("Ed25519");
            PrivateKey decodedPrivate = factory.generatePrivate(
                    new PKCS8EncodedKeySpec(decodeBase64(privateValue)));
            PublicKey decodedPublic = factory.generatePublic(
                    new X509EncodedKeySpec(decodeBase64(publicValue)));
            return new KeyPair(decodedPublic, decodedPrivate);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Las claves Ed25519 configuradas no son válidas.",
                    exception);
        }
    }

    private Map<String, PublicKey> loadVerificationKeys(
            TicketingProperties properties,
            String activeKeyId,
            PublicKey activePublicKey) {
        Map<String, PublicKey> keys = new LinkedHashMap<>();
        keys.put(activeKeyId, activePublicKey);
        String configured = properties.getVerificationPublicKeys();
        if (configured == null || configured.isBlank()) {
            return Map.copyOf(keys);
        }
        for (String entry : configured.split(",")) {
            String[] parts = entry.trim().split("=", 2);
            if (parts.length != 2 || parts[0].isBlank()
                    || parts[1].isBlank()) {
                throw new IllegalStateException(
                        "TICKETING_VERIFICATION_PUBLIC_KEYS debe usar "
                        + "el formato key-id=base64,key-id-2=base64.");
            }
            String verificationKeyId = parts[0].trim();
            PublicKey publicKey = decodePublicKey(parts[1]);
            PublicKey previous = keys.putIfAbsent(
                    verificationKeyId,
                    publicKey);
            if (previous != null && !verificationKeyId.equals(activeKeyId)) {
                throw new IllegalStateException(
                        "Cada identificador de clave de verificación debe ser único.");
            }
        }
        return Map.copyOf(keys);
    }

    private PublicKey decodePublicKey(String value) {
        try {
            KeyFactory factory = KeyFactory.getInstance("Ed25519");
            return factory.generatePublic(
                    new X509EncodedKeySpec(decodeBase64(value)));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Una clave pública Ed25519 de verificación no es válida.",
                    exception);
        }
    }

    private byte[] decodeBase64(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("base64:")) {
            normalized = normalized.substring("base64:".length());
        }
        return Base64.getDecoder().decode(normalized);
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String requireText(String value, String property) {
        if (!hasText(value)) {
            throw new IllegalStateException(property + " es obligatorio.");
        }
        return value.trim();
    }
}
