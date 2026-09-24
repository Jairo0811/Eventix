package com.jairomatias.eventix.venue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VenueForm {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 160, message = "El nombre no puede superar 160 caracteres.")
    private String name;

    @NotBlank(message = "La dirección es obligatoria.")
    @Size(max = 300, message = "La dirección no puede superar 300 caracteres.")
    private String address;

    @NotBlank(message = "La ciudad es obligatoria.")
    @Size(max = 120, message = "La ciudad no puede superar 120 caracteres.")
    private String city;

    @NotBlank(message = "El código de país es obligatorio.")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Usa un código ISO de 2 letras, por ejemplo DO.")
    private String countryCode = "DO";

    @NotBlank(message = "La zona horaria es obligatoria.")
    @Size(max = 80, message = "La zona horaria no puede superar 80 caracteres.")
    private String timeZone = "America/Santo_Domingo";

    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
