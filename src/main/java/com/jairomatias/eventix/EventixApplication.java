package com.jairomatias.eventix;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableMethodSecurity
@EnableScheduling
@EnableAsync
public class EventixApplication {

    private static final String DEFAULT_TIME_ZONE = "America/Santo_Domingo";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
        SpringApplication.run(EventixApplication.class, args);
    }
}
