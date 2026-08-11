package com.jairomatias.eventix.notification.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "eventix.notifications.reminders")
@Validated
public class ReminderProperties {

    private boolean enabled;
    @NotNull
    private Duration advance = Duration.ofHours(24);
    @NotNull
    private Duration retryDelay = Duration.ofMinutes(15);
    @Min(1)
    private int maximumAttempts = 5;
    @Min(1)
    private int candidateBatchSize = 500;
    @Min(1)
    private int deliveryBatchSize = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getAdvance() {
        return advance;
    }

    public void setAdvance(Duration advance) {
        this.advance = advance;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public void setMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts;
    }

    public int getCandidateBatchSize() {
        return candidateBatchSize;
    }

    public void setCandidateBatchSize(int candidateBatchSize) {
        this.candidateBatchSize = candidateBatchSize;
    }

    public int getDeliveryBatchSize() {
        return deliveryBatchSize;
    }

    public void setDeliveryBatchSize(int deliveryBatchSize) {
        this.deliveryBatchSize = deliveryBatchSize;
    }
}
