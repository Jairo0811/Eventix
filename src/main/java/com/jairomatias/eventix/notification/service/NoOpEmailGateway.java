package com.jairomatias.eventix.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "eventix.notifications.email",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class NoOpEmailGateway
        implements EmailGateway, EmailAttachmentGateway {

    @Override
    public void send(String recipient, String subject, String body) {
        // Email delivery is intentionally disabled for this environment.
    }

    @Override
    public void sendWithAttachments(
            String recipient,
            String subject,
            String body,
            java.util.List<EmailAttachment> attachments) {
        // Email delivery is intentionally disabled for this environment.
    }
}
