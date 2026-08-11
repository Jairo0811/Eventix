package com.jairomatias.eventix.notification.config;

import com.jairomatias.eventix.notification.service.NotificationProperties;
import com.jairomatias.eventix.notification.service.ReminderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        NotificationProperties.class,
        ReminderProperties.class
})
public class NotificationConfiguration {
}
