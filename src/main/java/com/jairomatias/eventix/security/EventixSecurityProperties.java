package com.jairomatias.eventix.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eventix.security")
public class EventixSecurityProperties {

    private boolean allowDefaultBootstrapAdministrator = true;
    private String bootstrapAdministratorPassword = "";
    private final RateLimit rateLimit = new RateLimit();
    private final Social social = new Social();

    public boolean isAllowDefaultBootstrapAdministrator() {
        return allowDefaultBootstrapAdministrator;
    }

    public void setAllowDefaultBootstrapAdministrator(boolean value) {
        allowDefaultBootstrapAdministrator = value;
    }

    public String getBootstrapAdministratorPassword() {
        return bootstrapAdministratorPassword;
    }

    public void setBootstrapAdministratorPassword(String value) {
        bootstrapAdministratorPassword = value;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Social getSocial() {
        return social;
    }

    public static class Social {
        private boolean enabled;
        private final Provider google = new Provider();
        private final AppleProvider apple = new AppleProvider();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Provider getGoogle() {
            return google;
        }

        public AppleProvider getApple() {
            return apple;
        }

        public boolean hasConfiguredProvider() {
            return google.isConfigured() || apple.isConfigured();
        }
    }

    public static class Provider {
        private String clientId = "";
        private String clientSecret = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public boolean isConfigured() {
            return !clientId.isBlank() && !clientSecret.isBlank();
        }
    }

    public static class AppleProvider extends Provider {
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int loginRequestsPerMinute = 10;
        private int walletRequestsPerMinute = 120;
        private int mutationRequestsPerMinute = 180;
        private int maximumTrackedClients = 10_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getLoginRequestsPerMinute() {
            return loginRequestsPerMinute;
        }

        public void setLoginRequestsPerMinute(int value) {
            loginRequestsPerMinute = positive(value);
        }

        public int getWalletRequestsPerMinute() {
            return walletRequestsPerMinute;
        }

        public void setWalletRequestsPerMinute(int value) {
            walletRequestsPerMinute = positive(value);
        }

        public int getMutationRequestsPerMinute() {
            return mutationRequestsPerMinute;
        }

        public void setMutationRequestsPerMinute(int value) {
            mutationRequestsPerMinute = positive(value);
        }

        public int getMaximumTrackedClients() {
            return maximumTrackedClients;
        }

        public void setMaximumTrackedClients(int value) {
            maximumTrackedClients = positive(value);
        }

        private int positive(int value) {
            if (value < 1) {
                throw new IllegalArgumentException(
                        "Los límites de solicitudes deben ser positivos.");
            }
            return value;
        }
    }
}
