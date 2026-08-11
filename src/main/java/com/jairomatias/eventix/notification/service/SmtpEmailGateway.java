package com.jairomatias.eventix.notification.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
@ConditionalOnProperty(
        prefix = "eventix.notifications.email",
        name = "enabled",
        havingValue = "true")
public class SmtpEmailGateway
        implements EmailGateway, EmailAttachmentGateway {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpEmailGateway(
            JavaMailSender mailSender,
            NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    public void sendWithAttachments(
            String recipient,
            String subject,
            String body,
            List<EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            send(recipient, subject, body);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            for (EmailAttachment attachment : attachments) {
                helper.addAttachment(
                        attachment.filename(),
                        new ByteArrayResource(attachment.content()),
                        attachment.contentType());
            }
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "No se pudo construir el correo con boletas.",
                    exception);
        }
    }
}
