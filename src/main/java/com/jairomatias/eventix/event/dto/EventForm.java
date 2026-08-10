package com.jairomatias.eventix.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.jairomatias.eventix.event.entity.EventStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EventForm {

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 160, message = "El título no puede superar 160 caracteres.")
    private String title;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(
            max = 5000,
            message = "La descripción no puede superar 5,000 caracteres.")
    private String description;

    @NotNull(message = "Selecciona una categoría.")
    private Long categoryId;

    @NotNull(message = "Selecciona un estado.")
    private EventStatus status = EventStatus.DRAFT;

    @NotNull(message = "La fecha y hora de inicio son obligatorias.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "La fecha y hora de finalización son obligatorias.")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @NotBlank(message = "El lugar es obligatorio.")
    @Size(max = 160, message = "El lugar no puede superar 160 caracteres.")
    private String venue;

    @NotBlank(message = "La dirección es obligatoria.")
    @Size(max = 300, message = "La dirección no puede superar 300 caracteres.")
    private String address;

    @Size(
            max = 1000,
            message = "El enlace de Google Maps no puede superar 1,000 caracteres.")
    @Pattern(
            regexp = "^(|https://(?:"
                    + "(?:www\\.)?google\\.com/maps(?:/.*)?|"
                    + "maps\\.google\\.com(?:/.*)?|"
                    + "maps\\.app\\.goo\\.gl(?:/.*)?|"
                    + "goo\\.gl/maps(?:/.*)?))$",
            message = "Usa un enlace HTTPS válido de Google Maps.")
    private String googleMapsUrl;

    @NotNull(message = "La capacidad es obligatoria.")
    @Min(value = 1, message = "La capacidad mínima es 1.")
    @Max(value = 1000000, message = "La capacidad máxima es 1,000,000.")
    private Integer capacity;

    @NotNull(message = "Selecciona un organizador responsable.")
    private Long organizerId;

    @Size(max = 500, message = "La URL no puede superar 500 caracteres.")
    @Pattern(
            regexp = "^(|https?://.+)$",
            message = "La portada debe usar una URL HTTP o HTTPS válida.")
    private String coverImageUrl;

    @NotNull
    private Boolean freeEvent = true;

    @NotNull(message = "Indica el precio base.")
    @DecimalMin(
            value = "0.00",
            message = "El precio no puede ser negativo.")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "El precio admite hasta 10 enteros y 2 decimales.")
    private BigDecimal basePrice = BigDecimal.ZERO;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGoogleMapsUrl() {
        return googleMapsUrl;
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public Boolean getFreeEvent() {
        return freeEvent;
    }

    public void setFreeEvent(Boolean freeEvent) {
        this.freeEvent = freeEvent;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
}
