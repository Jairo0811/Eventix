package com.jairomatias.eventix.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.jairomatias.eventix.audit.service.AuditService;

class GlobalExceptionHandlerTest {

    @Test
    void shouldRenderNotFoundForMissingStaticResourceWithoutAuditingAsServerError() {
        AuditService auditService = mock(AuditService.class);
        GlobalExceptionHandler handler = new GlobalExceptionHandler(auditService);
        NoResourceFoundException exception = mock(NoResourceFoundException.class);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = handler.handleNoResourceFound(exception, model);

        assertThat(view).isEqualTo("error/404");
        assertThat(model.get("message"))
                .isEqualTo("No se encontró el recurso solicitado.");
        verifyNoInteractions(auditService);
    }
}
