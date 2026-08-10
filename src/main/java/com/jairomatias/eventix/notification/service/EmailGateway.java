package com.jairomatias.eventix.notification.service;

public interface EmailGateway {

    void send(String recipient, String subject, String body);
}
