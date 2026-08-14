package com.jairomatias.eventix.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AccessibilityFoundationTest {

    @Test
    void sharedHeadDeclaresSpanishAndLoadsAccessibilityLayer() throws IOException {
        String head = resource("templates/fragments/head.html");
        assertThat(head).contains("<html lang=\"es\"");
        assertThat(head).contains("/css/accessibility.css");
        assertThat(head).contains("<title th:text=");
    }

    @Test
    void sidebarExposesNavigationLandmarkAndCurrentPage() throws IOException {
        String sidebar = resource("templates/fragments/sidebar.html");
        assertThat(sidebar).contains("aria-label=\"Navegación principal\"");
        assertThat(sidebar).contains("aria-current=${activePage");
        assertThat(sidebar).contains("aria-hidden=\"true\"");
    }

    @Test
    void transactionalMessagesAreAnnouncedToAssistiveTechnology() throws IOException {
        String messages = resource("templates/fragments/messages.html");
        assertThat(messages).contains("aria-live=\"polite\"");
        assertThat(messages).contains("role=\"status\"");
        assertThat(messages).contains("role=\"alert\"");
        assertThat(messages).contains("aria-live=\"assertive\"");
    }

    @Test
    void accessibilityStylesProtectFocusContrastZoomAndMotionPreferences() throws IOException {
        String css = resource("static/css/accessibility.css");
        assertThat(css).contains(":focus-visible");
        assertThat(css).contains("--eventix-muted-accessible");
        assertThat(css).contains("min-height: 44px");
        assertThat(css).contains("prefers-reduced-motion: reduce");
        assertThat(css).contains("max-width: 100%");
    }

    @Test
    void authenticationFormsAssociateValidationErrorsWithInputs() throws IOException {
        String forgot = resource("templates/auth/forgot-password.html");
        String reset = resource("templates/auth/reset-password.html");
        String change = resource("templates/auth/change-password.html");
        assertThat(forgot).contains("th:aria-invalid=\"${#fields.hasErrors('email')}\"")
                .contains("aria-describedby=\"email-error\"")
                .contains("id=\"email-error\" role=\"alert\"");
        assertThat(reset).contains("aria-describedby=\"newPassword-help newPassword-error\"")
                .contains("id=\"confirmPassword-error\" role=\"alert\"");
        assertThat(change).contains("aria-describedby=\"currentPassword-error\"")
                .contains("aria-live=\"assertive\"");
    }

    @Test
    void loginAnnouncesAuthenticationStateAndExposesMainContent() throws IOException {
        String login = resource("templates/auth/login.html");
        assertThat(login).contains("<main class=\"auth-page\" id=\"main-content\"")
                .contains("aria-labelledby=\"login-title\"")
                .contains("aria-live=\"assertive\"")
                .contains("aria-live=\"polite\"");
    }

    @Test
    void checkoutAssociatesErrorsAndProvidesTransactionalReviewGuidance() throws IOException {
        String checkout = resource("templates/checkout/form.html");
        assertThat(checkout).contains("<main class=\"app-main\" id=\"main-content\"")
                .contains("<fieldset class=\"border-0 p-0 m-0\"")
                .contains("aria-describedby=\"quantity-help quantity-error\"")
                .contains("id=\"firstName-error\"")
                .contains("id=\"provider-error\"")
                .contains("aria-live=\"assertive\"")
                .contains("Antes de confirmar")
                .contains("oportunidad de corregirlo antes de que la operación sea aceptada");
    }

    @Test
    void eventFormAssociatesErrorsHelpAndMapStatus() throws IOException {
        String form = resource("templates/events/form.html");
        assertThat(form).contains("<main class=\"app-main\" id=\"main-content\"")
                .contains("aria-labelledby=\"event-form-title\"")
                .contains("aria-describedby=\"event-form-instructions\"")
                .contains("th:aria-invalid=\"${#fields.hasErrors('title')}\"")
                .contains("aria-describedby=\"status-help status-error\"")
                .contains("aria-describedby=\"googleMapsUrl-help googleMapsUrl-error\"")
                .contains("id=\"coverImage-help\"")
                .contains("role=\"region\"")
                .contains("aria-label=\"Vista previa de ubicación del evento\"")
                .contains("role=\"status\"")
                .contains("aria-live=\"polite\"")
                .contains("bg-body p-3");
    }

    @Test
    void userFormExposesRequiredFieldsValidationAndSecurityGuidance() throws IOException {
        String form = resource("templates/users/form.html");
        assertThat(form).contains("<main class=\"app-main\" id=\"main-content\"")
                .contains("aria-labelledby=\"user-form-title\"")
                .contains("aria-describedby=\"user-form-instructions\"")
                .contains("th:aria-invalid=\"${#fields.hasErrors('firstName')}\"")
                .contains("aria-describedby=\"username-help username-error\"")
                .contains("aria-describedby=\"roleName-help roleName-error\"")
                .contains("aria-describedby=\"password-help password-error\"")
                .contains("id=\"password-error\" role=\"alert\"")
                .contains("Cambiar el estado puede afectar la capacidad del usuario para iniciar sesión");
    }

    @Test
    void profileAssociatesErrorsPreferencesAndAccountSecurity() throws IOException {
        String profile = resource("templates/profile/index.html");
        assertThat(profile).contains("<main class=\"app-main\" id=\"main-content\"")
                .contains("id=\"profile-title\"")
                .contains("aria-describedby=\"profile-instructions\"")
                .contains("aria-describedby=\"email-help email-error\"")
                .contains("th:aria-invalid=\"${#fields.hasErrors('email')}\"")
                .contains("<fieldset class=\"border-0 p-0 m-0\"")
                .contains("aria-describedby=\"reservationNotifications-help\"")
                .contains("id=\"notification-security-note\" role=\"note\"")
                .contains("aria-describedby=\"security-description\"");
    }

    @Test
    void settlementFormProvidesFinancialReviewAndAccessibleValidation() throws IOException {
        String form = resource("templates/settlements/form.html");
        assertThat(form).contains("<main class=\"app-main\" id=\"main-content\"")
                .contains("aria-labelledby=\"settlement-form-title\"")
                .contains("settlement-financial-help")
                .contains("aria-live=\"assertive\"")
                .contains("th:aria-invalid=\"${#fields.hasErrors('organizerId')}\"")
                .contains("aria-describedby=\"organizerId-help organizerId-error\"")
                .contains("<fieldset class=\"border-0 p-0 m-0 mb-4\"")
                .contains("id=\"periodFrom-error\" role=\"alert\"")
                .contains("id=\"periodTo-error\" role=\"alert\"")
                .contains("Antes de crearla:")
                .contains("podrás verificar el cálculo resultante antes de registrar estados de pago");
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as("classpath resource %s", path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
