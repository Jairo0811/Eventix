package com.jairomatias.eventix.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileUpdateForm {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres.")
    private String lastName;

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "Introduce un correo válido.")
    @Size(max = 160, message = "El correo no puede superar 160 caracteres.")
    private String email;

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres.")
    private String phone;

    private boolean reservationNotificationsEnabled;
    private boolean eventReminderNotificationsEnabled;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isReservationNotificationsEnabled() {
        return reservationNotificationsEnabled;
    }

    public void setReservationNotificationsEnabled(
            boolean reservationNotificationsEnabled) {
        this.reservationNotificationsEnabled = reservationNotificationsEnabled;
    }

    public boolean isEventReminderNotificationsEnabled() {
        return eventReminderNotificationsEnabled;
    }

    public void setEventReminderNotificationsEnabled(
            boolean eventReminderNotificationsEnabled) {
        this.eventReminderNotificationsEnabled = eventReminderNotificationsEnabled;
    }
}
