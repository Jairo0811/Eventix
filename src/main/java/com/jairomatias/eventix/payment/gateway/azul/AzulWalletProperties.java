package com.jairomatias.eventix.payment.gateway.azul;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "eventix.payments.azul-wallet")
public class AzulWalletProperties {

    private boolean enabled;
    private Environment environment = Environment.TEST;
    private String store;
    private String auth1;
    private String auth2;
    private String channel = "EC";
    private String merchantDisplayName = "Eventix";
    private String initiativeContext;
    private String googleMerchantId;
    private String googlePayMerchantId;
    private String appleDomainAssociation;

    public boolean isReady() {
        return enabled
                && StringUtils.hasText(store)
                && StringUtils.hasText(auth1)
                && StringUtils.hasText(auth2);
    }

    public boolean isGooglePayReady() {
        return isReady()
                && StringUtils.hasText(googleGatewayMerchantId())
                && (environment == Environment.TEST
                        || StringUtils.hasText(googlePayMerchantId));
    }

    public String soapEndpoint() {
        return environment == Environment.PRODUCTION
                ? "https://pagos.azul.com.do/webservices/SOAP/Default.asmx"
                : "https://pruebas.azul.com.do/webservices/SOAP/Default.asmx";
    }

    public String googleGatewayMerchantId() {
        return StringUtils.hasText(googleMerchantId) ? googleMerchantId : store;
    }

    public String googlePayMerchantId() {
        return googlePayMerchantId;
    }

    public String appleMerchantIdentifier() {
        return environment == Environment.PRODUCTION
                ? "platformintegrator.pagos.azul.apple.pay"
                : "payplatformintegrator.pruebas.azul.apple.pay";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public String getAuth1() {
        return auth1;
    }

    public void setAuth1(String auth1) {
        this.auth1 = auth1;
    }

    public String getAuth2() {
        return auth2;
    }

    public void setAuth2(String auth2) {
        this.auth2 = auth2;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMerchantDisplayName() {
        return merchantDisplayName;
    }

    public void setMerchantDisplayName(String merchantDisplayName) {
        this.merchantDisplayName = merchantDisplayName;
    }

    public String getInitiativeContext() {
        return initiativeContext;
    }

    public void setInitiativeContext(String initiativeContext) {
        this.initiativeContext = initiativeContext;
    }

    public String getGoogleMerchantId() {
        return googleMerchantId;
    }

    public void setGoogleMerchantId(String googleMerchantId) {
        this.googleMerchantId = googleMerchantId;
    }

    public String getGooglePayMerchantId() {
        return googlePayMerchantId;
    }

    public void setGooglePayMerchantId(String googlePayMerchantId) {
        this.googlePayMerchantId = googlePayMerchantId;
    }

    public String getAppleDomainAssociation() {
        return appleDomainAssociation;
    }

    public void setAppleDomainAssociation(String appleDomainAssociation) {
        this.appleDomainAssociation = appleDomainAssociation;
    }

    public enum Environment {
        TEST,
        PRODUCTION
    }
}
