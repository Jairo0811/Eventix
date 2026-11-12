package com.jairomatias.eventix.home.controller;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.jairomatias.eventix.home.dto.HomeEventView;
import com.jairomatias.eventix.home.dto.PublicEventDetail;
import com.jairomatias.eventix.home.service.IcsCalendarWriter;
import com.jairomatias.eventix.home.service.PublicStorefrontService;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;

/** Tienda pública: se puede explorar y compartir sin iniciar sesión. */
@Controller
public class PublicEventController {

    private static final int SITEMAP_CACHE_MINUTES = 10;
    private static final String NOT_FOUND = "No se encontró el evento solicitado.";

    private final PublicStorefrontService storefront;
    private final String baseUrl;

    public PublicEventController(
            PublicStorefrontService storefront,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.storefront = storefront;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @GetMapping("/e")
    public String list(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false, defaultValue = "all") String when,
            @RequestParam(required = false, defaultValue = "0") int page,
            Model model) {
        model.addAttribute("events", storefront.search(term, category, when, page));
        model.addAttribute("categories", storefront.categories());
        model.addAttribute("term", term == null ? "" : term.trim());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("when", when);
        model.addAttribute("canonicalUrl", baseUrl + "/e");
        model.addAttribute("ogImage", defaultShareImage());
        return "public/events";
    }

    @GetMapping("/e/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PublicEventDetail event = storefront.findEvent(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        String url = baseUrl + "/e/" + event.id();
        String cover = absoluteImage(event.coverImageUrl());
        model.addAttribute("event", event);
        model.addAttribute("canonicalUrl", url);
        model.addAttribute("shareUrl", url);
        model.addAttribute("ogImage", cover != null ? cover : defaultShareImage());
        model.addAttribute("jsonLd", jsonLd(event, url));
        return "public/event-detail";
    }

    @GetMapping("/e/{id}/calendar.ics")
    public ResponseEntity<byte[]> calendar(@PathVariable Long id) {
        PublicEventDetail event = storefront.findEvent(id)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        String location = event.address() == null || event.address().isBlank()
                ? event.venue()
                : event.venue() + ", " + event.address();
        String ics = IcsCalendarWriter.forEvent(
                event.id(),
                event.title(),
                location,
                baseUrl + "/e/" + event.id(),
                event.startAt(),
                event.endAt(),
                PublicEventDetail.EVENT_ZONE,
                ZonedDateTime.now(ZoneOffset.UTC));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("evento-" + event.id() + ".ics")
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        appendUrl(xml, baseUrl + "/");
        appendUrl(xml, baseUrl + "/e");
        for (HomeEventView event : storefront.sitemapEvents()) {
            appendUrl(xml, baseUrl + "/e/" + event.id());
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(SITEMAP_CACHE_MINUTES)).cachePublic())
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    @GetMapping("/robots.txt")
    public ResponseEntity<String> robots() {
        String body = String.join("\n",
                "User-agent: *",
                "Allow: /",
                "Disallow: /my/",
                "Disallow: /dashboard",
                "Disallow: /login",
                "Disallow: /profile",
                "Disallow: /tickets/",
                "Disallow: /api/",
                "Sitemap: " + baseUrl + "/sitemap.xml") + "\n";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    private Map<String, Object> jsonLd(PublicEventDetail event, String url) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("@context", "https://schema.org");
        data.put("@type", "Event");
        data.put("name", event.title());
        data.put("startDate", event.startIso());
        data.put("endDate", event.endIso());
        data.put("eventStatus", "https://schema.org/EventScheduled");
        data.put("eventAttendanceMode", "https://schema.org/OfflineEventAttendanceMode");
        data.put("description", event.summary());
        data.put("url", url);
        Map<String, Object> place = new LinkedHashMap<>();
        place.put("@type", "Place");
        place.put("name", event.venue());
        if (event.address() != null && !event.address().isBlank()) {
            place.put("address", event.address());
        }
        data.put("location", place);
        String image = absoluteImage(event.coverImageUrl());
        if (image != null) {
            data.put("image", List.of(image));
        }
        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("@type", "Offer");
        offer.put("url", url);
        offer.put("priceCurrency", "DOP");
        offer.put("price", event.freeEvent() || event.basePrice() == null
                ? "0" : event.basePrice().toPlainString());
        offer.put("availability", event.onSale()
                ? "https://schema.org/InStock" : "https://schema.org/SoldOut");
        data.put("offers", offer);
        return data;
    }

    private String defaultShareImage() {
        return baseUrl + "/images/eventix-share.png";
    }

    private String absoluteImage(String cover) {
        if (cover == null || cover.isBlank()) {
            return null;
        }
        if (cover.startsWith("https://")) {
            return cover;
        }
        return cover.startsWith("/") ? baseUrl + cover : null;
    }

    private static void appendUrl(StringBuilder xml, String location) {
        xml.append("  <url><loc>").append(location.replace("&", "&amp;")).append("</loc></url>\n");
    }
}
