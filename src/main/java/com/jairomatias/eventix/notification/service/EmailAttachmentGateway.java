package com.jairomatias.eventix.notification.service;

import java.util.List;

public interface EmailAttachmentGateway {

    void sendWithAttachments(
            String recipient,
            String subject,
            String body,
            List<EmailAttachment> attachments);
}
