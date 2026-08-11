package com.jairomatias.eventix.notification.service;

public record EmailAttachment(
        String filename,
        String contentType,
        byte[] content) {
}
