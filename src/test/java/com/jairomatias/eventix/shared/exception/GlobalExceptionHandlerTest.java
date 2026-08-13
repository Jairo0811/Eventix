package com.jairomatias.eventix.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jairomatias.eventix.audit.service.AuditService;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private AuditService auditService;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(auditService);
    }

    @Test
    void shouldRenderNotFoundForMissingStaticResourceWithoutAuditingAsServerError() {
        NoResourceFoundException exception = mock(NoResourceFoundException.class);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = handler.handleNoResourceFound(exception, model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.get("message"))
                .isEqualTo("No se encontró el recurso solicitado.");
        verifyNoInteractions(auditService);
    }
}
