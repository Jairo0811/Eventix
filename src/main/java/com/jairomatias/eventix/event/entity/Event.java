package com.jairomatias.eventix.event.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.jairomatias.eventix.category.entity.EventCategory;
import com.jairomatias.eventix.shared.entity.AuditableEntity;
import com.jairomatias.eventix.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event extends AuditableEntity {

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, length = 160)
    private String venue;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(name = "google_maps_url", length = 1000)
    private String googleMapsUrl;

    @Column(nullable = false)
    private int capacity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "is_free", nullable = false)
    private boolean freeEvent = true;

    @Column(
            name = "base_price",
            nullable = false,
            precision = 12,
            scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    protected Event() {
    }

    public Event(
            String title,
            String description,
            EventCategory category,
            EventStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String venue,
            String address,
            int capacity,
            User organizer,
            String coverImageUrl,
            boolean freeEvent,
            BigDecimal basePrice) {

        update(
                title,
                description,
                category,
                status,
                startAt,
                endAt,
                venue,
                address,
                capacity,
                organizer,
                coverImageUrl,
                freeEvent,
                basePrice);
    }

    public void update(
            String title,
            String description,
            EventCategory category,
            EventStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String venue,
            String address,
            int capacity,
            User organizer,
            String coverImageUrl,
            boolean freeEvent,
            BigDecimal basePrice) {

        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.venue = venue;
        this.address = address;
        this.capacity = capacity;
        this.organizer = organizer;
        this.coverImageUrl = coverImageUrl;
        this.freeEvent = freeEvent;
        this.basePrice = basePrice;
    }

    public void updateGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public String getVenue() {
        return venue;
    }

    public String getAddress() {
        return address;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public int getCapacity() {
        return capacity;
    }

    public User getOrganizer() {
        return organizer;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public boolean isFreeEvent() {
        return freeEvent;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }
}
