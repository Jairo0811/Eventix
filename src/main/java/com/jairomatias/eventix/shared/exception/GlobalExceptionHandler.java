package com.jairomatias.eventix.shared.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jairomatias.eventix.audit.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String NOT_FOUND_MESSAGE = "No se encontró el recurso solicitado.";

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/404";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(NoResourceFoundException exception, Model model) {
        LOGGER.debug("Recurso no encontrado: {}", exception.getResourcePath());
        model.addAttribute("message", NOT_FOUND_MESSAGE);
        return "error/404";
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessRule(BusinessRuleException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/400";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(
            ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "No fue posible completar la solicitud."
                : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpectedException(
            Exception exception,
            Model model,
            HttpServletRequest request) {
        LOGGER.error("Error no controlado al procesar la solicitud", exception);
        auditService.recordError(exception, request);
        model.addAttribute("message", "No fue posible completar la operación.");
        return "error/500";
    }
}
