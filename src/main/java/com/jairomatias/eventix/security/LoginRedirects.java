package com.jairomatias.eventix.security;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Valida el destino {@code next} que se conserva al pasar por el login (por
 * ejemplo, al pulsar "Comprar" siendo visitante). Solo se aceptan rutas
 * internas de la tienda y del área del comprador, nunca URL absolutas.
 */
public final class LoginRedirects {

    private static final Pattern ALLOWED = Pattern.compile(
            "^/(my|e)(/[A-Za-z0-9._~%\\-]+)*$");

    private LoginRedirects() {
    }

    public static Optional<String> safeNext(String candidate) {
        if (candidate == null || candidate.length() > 200 || candidate.contains("..")) {
            return Optional.empty();
        }
        return ALLOWED.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
    }
}
