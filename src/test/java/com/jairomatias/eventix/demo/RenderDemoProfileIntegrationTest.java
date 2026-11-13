package com.jairomatias.eventix.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.jairomatias.eventix.event.repository.EventRepository;
import com.jairomatias.eventix.user.repository.UserRepository;

/**
 * Actual Render-like application startup: H2 create-drop, repositories,
 * startup runner, Thymeleaf views, security filter chain, and HTTP routes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("render")
@TestPropertySource(properties = {
        "EVENTIX_ELIGIBILITY_HMAC_SECRET=render-only-testing-hmac-secret-0123456789",
        "EVENTIX_EMAIL_ENABLED=false",
        "GOOGLE_WALLET_ENABLED=false",
        "APPLE_WALLET_ENABLED=false",
        "AZUL_WALLET_ENABLED=false"
})
class RenderDemoProfileIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EventRepository eventRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void renderProfileSeedsThreeEventsWithoutActiveOrganizerAccess() {
        assertThat(eventRepository.count()).isEqualTo(3L);
        assertThat(userRepository.count()).isEqualTo(1L);
        assertThat(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "organizador-demo@eventix.local", "eventix_demo")).isPresent()
                .get().extracting(user -> user.getStatus().name())
                .isEqualTo("INACTIVE");
    }

    @Test
    void renderHomeAndCatalogClearlyIdentifyFictionalEvents() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DEMOSTRACIÓN")))
                .andExpect(content().string(containsString("noindex, nofollow")))
                .andExpect(content().string(containsString("Eventix Tech Experience")))
                .andExpect(content().string(containsString("Explora Eventix con eventos de demostración.")))
                .andExpect(content().string(not(containsString("href=\"/login\""))));

        mockMvc.perform(get("/e"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Noche de Música")))
                .andExpect(content().string(containsString("Encuentro Creativo")))
                .andExpect(content().string(containsString("no se realizan cobros reales")));

        Long eventId = eventRepository.findAll().get(0).getId();
        mockMvc.perform(get("/e/" + eventId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solo demostración.")))
                .andExpect(content().string(not(containsString("Inicia sesión para comprar"))));
    }
}
