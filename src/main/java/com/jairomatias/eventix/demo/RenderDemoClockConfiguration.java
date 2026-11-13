package com.jairomatias.eventix.demo;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("render")
public class RenderDemoClockConfiguration {

    @Bean
    Clock renderDemoClock() {
        return Clock.system(ZoneId.of("America/Santo_Domingo"));
    }
}
