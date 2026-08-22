package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NationalIdLookupServiceTest {

    @Test
    void producesSameLookupForFormattedAndUnformattedNationalId() {
        NationalIdLookupService service = new NationalIdLookupService("test-secret");

        String formatted = service.lookupKey("001-1234567-8");
        String plain = service.lookupKey("00112345678");

        assertThat(formatted).isEqualTo(plain).hasSize(64);
        assertThat(service.last4("001-1234567-8")).isEqualTo("5678");
    }

    @Test
    void refusesLookupWhenSecretIsMissing() {
        NationalIdLookupService service = new NationalIdLookupService("");

        assertThatThrownBy(() -> service.lookupKey("00112345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EVENTIX_ELIGIBILITY_HMAC_SECRET");
    }

    @Test
    void refusesInvalidNationalIdLength() {
        NationalIdLookupService service = new NationalIdLookupService("test-secret");

        assertThatThrownBy(() -> service.lookupKey("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("11 dígitos");
    }
}
