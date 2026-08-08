package com.jairomatias.eventix.ticket.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eventix.ticketing")
public class TicketingProperties {

    private String issuerName = "Eventix";
    private String signingKeyId = "eventix-dev";
    private String signingPrivateKey = "";
    private String signingPublicKey = "";
    private boolean allowEphemeralSigningKey = true;
    private boolean allowReentry;
    private final GoogleWallet googleWallet = new GoogleWallet();
    private final AppleWallet appleWallet = new AppleWallet();

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getSigningKeyId() {
        return signingKeyId;
    }

    public void setSigningKeyId(String signingKeyId) {
        this.signingKeyId = signingKeyId;
    }

    public String getSigningPrivateKey() {
        return signingPrivateKey;
    }

    public void setSigningPrivateKey(String signingPrivateKey) {
        this.signingPrivateKey = signingPrivateKey;
    }

    public String getSigningPublicKey() {
        return signingPublicKey;
    }

    public void setSigningPublicKey(String signingPublicKey) {
        this.signingPublicKey = signingPublicKey;
    }

    public boolean isAllowEphemeralSigningKey() {
        return allowEphemeralSigningKey;
    }

    public void setAllowEphemeralSigningKey(boolean value) {
        allowEphemeralSigningKey = value;
    }

    public boolean isAllowReentry() {
        return allowReentry;
    }

    public void setAllowReentry(boolean allowReentry) {
        this.allowReentry = allowReentry;
    }

    public GoogleWallet getGoogleWallet() {
        return googleWallet;
    }

    public AppleWallet getAppleWallet() {
        return appleWallet;
    }

    public static class GoogleWallet {
        private boolean enabled;
        private String issuerId = "";
        private String serviceAccountJson = "";
        private List<String> origins = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuerId() {
            return issuerId;
        }

        public void setIssuerId(String issuerId) {
            this.issuerId = issuerId;
        }

        public String getServiceAccountJson() {
            return serviceAccountJson;
        }

        public void setServiceAccountJson(String value) {
            serviceAccountJson = value;
        }

        public List<String> getOrigins() {
            return origins;
        }

        public void setOrigins(List<String> origins) {
            this.origins = origins == null
                    ? new ArrayList<>()
                    : new ArrayList<>(origins);
        }

        public boolean configured() {
            return enabled
                    && !blank(issuerId)
                    && !blank(serviceAccountJson);
        }
    }

    public static class AppleWallet {
        private boolean enabled;
        private String passTypeIdentifier = "";
        private String teamIdentifier = "";
        private String certificateP12 = "";
        private String certificatePassword = "";
        private String wwdrCertificate = "";
        private String webServiceUrl = "";
        private boolean apnsEnabled;
        private boolean apnsProduction;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPassTypeIdentifier() {
            return passTypeIdentifier;
        }

        public void setPassTypeIdentifier(String value) {
            passTypeIdentifier = value;
        }

        public String getTeamIdentifier() {
            return teamIdentifier;
        }

        public void setTeamIdentifier(String value) {
            teamIdentifier = value;
        }

        public String getCertificateP12() {
            return certificateP12;
        }

        public void setCertificateP12(String value) {
            certificateP12 = value;
        }

        public String getCertificatePassword() {
            return certificatePassword;
        }

        public void setCertificatePassword(String value) {
            certificatePassword = value;
        }

        public String getWwdrCertificate() {
            return wwdrCertificate;
        }

        public void setWwdrCertificate(String value) {
            wwdrCertificate = value;
        }

        public String getWebServiceUrl() {
            return webServiceUrl;
        }

        public void setWebServiceUrl(String value) {
            webServiceUrl = value;
        }

        public boolean isApnsEnabled() {
            return apnsEnabled;
        }

        public void setApnsEnabled(boolean value) {
            apnsEnabled = value;
        }

        public boolean isApnsProduction() {
            return apnsProduction;
        }

        public void setApnsProduction(boolean value) {
            apnsProduction = value;
        }

        public boolean configured() {
            return enabled
                    && !blank(passTypeIdentifier)
                    && !blank(teamIdentifier)
                    && !blank(certificateP12)
                    && !blank(wwdrCertificate)
                    && !blank(webServiceUrl);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
