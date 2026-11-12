package com.jairomatias.eventix.home.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jairomatias.eventix.audit.service.AuditService;
import com.jairomatias.eventix.home.dto.HomeEventView;
import com.jairomatias.eventix.home.dto.PublicCategory;
import com.jairomatias.eventix.home.dto.PublicEventDetail;
import com.jairomatias.eventix.home.dto.PublicTicketOption;
import com.jairomatias.eventix.home.service.PublicStorefrontService;
import com.jairomatias.eventix.security.AuditAuthenticationFailureHandler;
import com.jairomatias.eventix.security.AuditLogoutSuccessHandler;
import com.jairomatias.eventix.security.DatabaseUserDetailsService;
import com.jairomatias.eventix.security.EventixSecurityProperties;
import com.jairomatias.eventix.security.ExternalOidcUserService;
import com.jairomatias.eventix.security.LoginSuccessHandler;
import com.jairomatias.eventix.security.SecurityConfig;

/**
 * Renderiza de verdad las plantillas públicas, que es la única forma de
 * detectar un error de expresión de Thymeleaf: la compilación no los ve.
 */
@WebMvcTest(PublicEventController.class)
@Import({SecurityConfig.class, EventixSecurityProperties.class})
class PublicEventControllerTest {

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

    private static final HomeEventView EVENT = new HomeEventView(
            7L,
            "Concierto de Año Nuevo",
            "Música",
            LocalDateTime.of(2030, 12, 31, 21, 0),
            "Teatro Nacional",
            null,
            false,
            new BigDecimal("1250.00"));

    @BeforeEach
    void stubCategories() {
        when(storefront.categories())
                .thenReturn(List.of(new PublicCategory(3L, "Música")));
    }

    private static PublicEventDetail detail(boolean onSale, List<PublicTicketOption> tickets) {
        return new PublicEventDetail(
                7L,
                "Concierto de Año Nuevo",
                "Una noche de música en vivo.",
                "Música",
                LocalDateTime.of(2030, 12, 31, 21, 0),
                LocalDateTime.of(2031, 1, 1, 1, 0),
                "Teatro Nacional",
                "Av. Máximo Gómez",
                "https://maps.google.com/?q=teatro",
                null,
                false,
                new BigDecimal("1250.00"),
                onSale,
                tickets);
    }

    @Test
    void catalogRendersWithoutAuthenticationAndShowsFormattedPrices() throws Exception {
        when(storefront.search(any(), any(), anyString(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(EVENT)));

        mockMvc.perform(get("/e"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/events"))
                .andExpect(content().string(containsString("Concierto de Año Nuevo")))
                .andExpect(content().string(containsString("RD$ 1,250.00")))
                .andExpect(content().string(containsString("Este fin de semana")));
    }

    @Test
    void emptyCatalogShowsItsOwnMessageInsteadOfABlankGrid() throws Exception {
        when(storefront.search(any(), any(), anyString(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/e"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No encontramos eventos")));
    }

    @Test
    void eventPageRendersTicketsAndInvitesAVisitorToSignInToBuy() throws Exception {
        when(storefront.findEvent(7L)).thenReturn(Optional.of(detail(
                true,
                List.of(new PublicTicketOption("General", "General", new BigDecimal("1250.00"), 5)))));

        mockMvc.perform(get("/e/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/event-detail"))
                .andExpect(content().string(containsString("Inicia sesión para comprar")))
                // El destino del checkout viaja en next para volver tras el login;
                // no se fija la codificación de la barra, que decide Thymeleaf.
                .andExpect(content().string(containsString("/login?next=")))
                .andExpect(content().string(containsString("checkout")))
                .andExpect(content().string(containsString("Quedan 5")))
                .andExpect(content().string(containsString("application/ld+json")));
    }

    @Test
    void soldOutEventOffersNoPurchaseButton() throws Exception {
        when(storefront.findEvent(7L)).thenReturn(Optional.of(detail(
                false,
                List.of(new PublicTicketOption("General", "General", new BigDecimal("1250.00"), 0)))));

        mockMvc.perform(get("/e/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Agotado")))
                .andExpect(content().string(not(containsString("Inicia sesión para comprar"))));
    }

    @Test
    void unpublishedEventLooksLikeItDoesNotExist() throws Exception {
        when(storefront.findEvent(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/e/404")).andExpect(status().isNotFound());
    }

    @Test
    void calendarDownloadIsAValidSingleEventIcs() throws Exception {
        when(storefront.findEvent(7L)).thenReturn(Optional.of(detail(true, List.of())));

        mockMvc.perform(get("/e/7/calendar.ics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(containsString("UID:event-7@eventix")))
                // Santo Domingo es UTC-4: 21:00 local del 31 es 01:00 UTC del 1.
                .andExpect(content().string(containsString("DTSTART:20310101T010000Z")))
                .andExpect(content().string(containsString("LOCATION:Teatro Nacional\\, Av. Máximo Gómez")));
    }

    @Test
    void robotsKeepsPrivateAreasOutAndPointsAtTheSitemap() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Disallow: /my/")))
                .andExpect(content().string(containsString("Disallow: /tickets/")))
                .andExpect(content().string(containsString("Sitemap:")));
    }

    @Test
    void sitemapListsTheCatalogAndEachPublishedEvent() throws Exception {
        when(storefront.sitemapEvents()).thenReturn(List.of(EVENT));

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<loc>")))
                .andExpect(content().string(containsString("/e/7</loc>")));
    }
}
