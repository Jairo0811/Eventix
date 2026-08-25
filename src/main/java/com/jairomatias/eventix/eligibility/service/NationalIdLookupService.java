package com.jairomatias.eventix.eligibility.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NationalIdLookupService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final String secret;

    public NationalIdLookupService(
            @Value("${eventix.eligibility.hmac-secret:}") String secret) {
        this.secret = secret == null ? "" : secret.trim();
    }

    public String lookupKey(String nationalId) {
        String normalized = normalize(nationalId);
        if (secret.isBlank()) {
            throw new IllegalStateException(
                    "EVENTIX_ELIGIBILITY_HMAC_SECRET debe configurarse antes de verificar cédulas.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length
                < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "EVENTIX_ELIGIBILITY_HMAC_SECRET debe contener al menos 32 bytes.");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("No se pudo proteger el identificador nacional.", exception);
        }
    }

    public String last4(String nationalId) {
        String normalized = normalize(nationalId);
        return normalized.substring(normalized.length() - 4);
    }

    private String normalize(String nationalId) {
        if (nationalId == null) {
            throw new IllegalArgumentException("La cédula es obligatoria.");
        }
        String normalized = nationalId.replaceAll("\\D", "");
        if (normalized.length() != 11) {
            throw new IllegalArgumentException("La cédula debe contener 11 dígitos.");
        }
        return normalized;
    }
}
