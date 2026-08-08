package com.jairomatias.eventix.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ScanForm {

    @NotBlank(message = "Escanea o introduce el código QR.")
    @Size(max = 512, message = "El contenido del QR no es válido.")
    private String token;

    @NotBlank(message = "Identifica el dispositivo de acceso.")
    @Size(max = 120, message = "El identificador no puede exceder 120 caracteres.")
    private String deviceIdentifier = "Navegador web";

    private boolean reentry;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String value) {
        deviceIdentifier = value;
    }

    public boolean isReentry() {
        return reentry;
    }

    public void setReentry(boolean reentry) {
        this.reentry = reentry;
    }
}
