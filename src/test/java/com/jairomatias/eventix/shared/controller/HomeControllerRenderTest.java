package com.jairomatias.eventix.shared.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jairomatias.eventix.audit.service.AuditService;
import com.jairomatias.eventix.home.dto.HomeEventView;
import com.jairomatias.eventix.home.dto.PublicCategory;
import com.jairomatias.eventix.home.service.EventDiscoveryService;
import com.jairomatias.eventix.home.service.PublicStorefrontService;
import com.jairomatias.eventix.security.AuditAuthenticationFailureHandler;
import com.jairomatias.eventix.security.AuditLogoutSuccessHandler;
import com.jairomatias.eventix.security.DatabaseUserDetailsService;
import com.jairomatias.eventix.security.EventixSecurityProperties;
import com.jairomatias.eventix.security.ExternalOidcUserService;
import com.jairomatias.eventix.security.LoginSuccessHandler;
import com.jairomatias.eventix.security.SecurityConfig;

/**
 * Exercises actual Thymeleaf rendering of the buyer-oriented commercial home.
 * Source-string checks alone do not detect invalid expressions or fragment errors.
 */
@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, EventixSecurityProperties.class})
class HomeControllerRenderTest {

    @MockitoBean
    private EventDiscoveryService discovery;

    @MockitoBean
    private PublicStorefrontService storefront;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private DatabaseUserDetailsService userDetailsService;

    @MockitoBean
    private LoginSuccessHandler loginSuccessHandler;

    @MockitoBean
    private AuditAuthenticationFailureHandler authenticationFailureHandler;

    @MockitoBean
    private AuditLogoutSuccessHandler logoutSuccessHandler;

    @MockitoBean
    private ExternalOidcUserService externalOidcUserService;

    @Autowired
    private MockMvc mockMvc;

    private static HomeEventView event(long id, String title, BigDecimal price) {
        return new HomeEventView(
                id,
                title,
                "Música",
                LocalDateTime.of(2030, 12, 31, 21, 0),
                "Teatro Nacional",
                null,
                price == null,
                price);
    }

    private void stubHome(List<HomeEventView> upcoming, List<PublicCategory> categories) {
        when(discovery.upcomingEvents()).thenReturn(upcoming);
        when(discovery.nextSevenEvents()).thenReturn(upcoming.stream().limit(7).toList());
        when(storefront.categories()).thenReturn(categories);
    }

    @Test
    void anonymousHomeRendersSearchAndUsefulEmptyState() throws Exception {
        stubHome(List.of(), List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(containsString("Compra boletas para tus eventos favoritos.")))
                .andExpect(content().string(containsString("La próxima experiencia está por anunciarse.")))
                .andExpect(content().string(containsString("action=\"/e\"")))
                .andExpect(content().string(containsString("href=\"/e?when=weekend\"")))
                .andExpect(content().string(containsString("lang=\"es\"")));
    }

    @Test
    void featuredEventRendersPriceAndLinksToPublicDetailInsteadOfLogin() throws Exception {
        stubHome(
                List.of(event(7L, "Concierto de Año Nuevo", new BigDecimal("1250.00"))),
                List.of(new PublicCategory(3L, "Música")));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Concierto de Año Nuevo")))
                .andExpect(content().string(containsString("RD$ 1,250.00")))
                .andExpect(content().string(containsString("href=\"/e/7\"")))
                .andExpect(content().string(containsString("href=\"/e?category=3\"")))
                .andExpect(content().string(not(containsString("href=\"/login?next="))));
    }

    @Test
    void additionalEventsRenderAsPublicCardsAndFreeEventShowsGratis() throws Exception {
        stubHome(
                List.of(
                        event(7L, "Concierto de Año Nuevo", new BigDecimal("1250.00")),
                        event(8L, "Encuentro gratuito", null)),
                List.of(new PublicCategory(3L, "Música")));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Más eventos en camino")))
                .andExpect(content().string(containsString("Encuentro gratuito")))
                .andExpect(content().string(containsString("href=\"/e/8\"")))
                .andExpect(content().string(containsString("Gratis")));
    }

    @Test
    @WithMockUser(username = "buyer@example.com", roles = "USER")
    void authenticatedBuyerSeesOwnTicketsNavigationWithoutLosingPublicDiscovery() throws Exception {
        stubHome(List.of(), List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/my/tickets\"")))
                .andExpect(content().string(containsString("Mis boletas")))
                .andExpect(content().string(containsString("action=\"/e\"")));
    }

    @Test
    void featuredTitleIsHtmlEscapedDuringServerRendering() throws Exception {
        stubHome(List.of(event(7L, "<script>alert(1)</script>", null)), List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }
}
