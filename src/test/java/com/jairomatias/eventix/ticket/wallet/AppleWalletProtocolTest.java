package com.jairomatias.eventix.ticket.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.jairomatias.eventix.ticket.controller.AppleWalletWebController;
import com.jairomatias.eventix.ticket.dto.ApplePassUpdates;

class AppleWalletProtocolTest {

    private static final String PASS_PATH =
            "/api/wallet/apple/v1/passes/pass.example/TKT-1";
    private final AppleWalletWebService service = mock(AppleWalletWebService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AppleWalletWebController(service)).build();
    }

    @Test
    void publishesBaseUrlWithoutProtocolVersion() {
        assertThat(DefaultAppleWalletPassService.webServiceBaseUrl(
                "https://example.com/api/wallet/apple/v1/"))
                .isEqualTo("https://example.com/api/wallet/apple");
        assertThat(DefaultAppleWalletPassService.webServiceBaseUrl(
                "https://example.com/api/wallet/apple/"))
                .isEqualTo("https://example.com/api/wallet/apple");
    }

    @Test
    void returnsNoContentForDeviceWithoutUpdates() throws Exception {
        when(service.findUpdates("device", "pass.example", null))
                .thenReturn(new ApplePassUpdates(List.of(), "0"));
        mvc.perform(get("/api/wallet/apple/v1/devices/device/registrations/pass.example"))
                .andExpect(status().isNoContent());
    }

    @Test
    void supportsPreviouslyIssuedVersionedUrls() throws Exception {
        when(service.findUpdates("device", "pass.example", null))
                .thenReturn(new ApplePassUpdates(List.of(), "0"));
        mvc.perform(get("/api/wallet/apple/v1/v1/devices/device/registrations/pass.example"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotModifiedWithoutSigningAgain() throws Exception {
        long modified = Instant.parse("2026-08-08T22:30:00Z").toEpochMilli();
        when(service.lastModified("pass.example", "TKT-1", "ApplePass secret"))
                .thenReturn(modified);
        mvc.perform(get(PASS_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "ApplePass secret")
                        .header(HttpHeaders.IF_MODIFIED_SINCE, "Sat, 08 Aug 2026 22:30:00 GMT"))
                .andExpect(status().isNotModified());
        verify(service, never()).latestPass("pass.example", "TKT-1", "ApplePass secret");
    }

    @Test
    void returnsUpdatedPassWithLastModified() throws Exception {
        long modified = Instant.parse("2026-08-08T22:30:00Z").toEpochMilli();
        when(service.lastModified("pass.example", "TKT-1", "ApplePass secret"))
                .thenReturn(modified);
        when(service.latestPass("pass.example", "TKT-1", "ApplePass secret"))
                .thenReturn(new byte[] {1, 2, 3});
        mvc.perform(get(PASS_PATH).header(HttpHeaders.AUTHORIZATION, "ApplePass secret"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.LAST_MODIFIED))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.pkpass"));
    }
}
