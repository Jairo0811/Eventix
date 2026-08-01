package com.jairomatias.eventix.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReservationForm {

    @NotNull(message = "Selecciona un evento.")
    private Long eventId;

    @NotBlank(message = "El nombre del asistente es obligatorio.")
    @Size(max = 80, message = "El nombre no puede exceder 80 caracteres.")
    private String attendeeFirstName;

    @NotBlank(message = "El apellido del asistente es obligatorio.")
    @Size(max = 80, message = "El apellido no puede exceder 80 caracteres.")
    private String attendeeLastName;

    @NotBlank(message = "El correo del asistente es obligatorio.")
    @Email(message = "Introduce un correo electrónico válido.")
    @Size(max = 160, message = "El correo no puede exceder 160 caracteres.")
    private String attendeeEmail;

    @NotBlank(message = "El teléfono del asistente es obligatorio.")
    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres.")
    private String attendeePhone;

    @Min(value = 1, message = "Debes reservar al menos una entrada.")
    @Max(value = 100, message = "Una reservación no puede superar 100 entradas.")
    private int quantity = 1;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getAttendeeFirstName() {
        return attendeeFirstName;
    }

    public void setAttendeeFirstName(String attendeeFirstName) {
        this.attendeeFirstName = attendeeFirstName;
    }

    public String getAttendeeLastName() {
        return attendeeLastName;
    }

    public void setAttendeeLastName(String attendeeLastName) {
        this.attendeeLastName = attendeeLastName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public void setAttendeeEmail(String attendeeEmail) {
        this.attendeeEmail = attendeeEmail;
    }

    public String getAttendeePhone() {
        return attendeePhone;
    }

    public void setAttendeePhone(String attendeePhone) {
        this.attendeePhone = attendeePhone;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
