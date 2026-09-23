package com.jairomatias.eventix.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@ConditionalOnProperty(
        prefix = "eventix.security.social",
        name = "enabled",
        havingValue = "true")
public class SocialLoginConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            EventixSecurityProperties properties) {
        List<ClientRegistration> registrations = new ArrayList<>();
        EventixSecurityProperties.Social social = properties.getSocial();

        if (social.getGoogle().isConfigured()) {
            registrations.add(google(social.getGoogle()));
        }
        if (social.getApple().isConfigured()) {
            registrations.add(apple(social.getApple()));
        }
        if (registrations.isEmpty()) {
            throw new IllegalStateException(
                    "Social login is enabled but no provider is configured.");
        }

        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    private ClientRegistration google(
            EventixSecurityProperties.Provider provider) {
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .clientName("Google")
                .scope("openid", "profile", "email")
                .build();
    }

    private ClientRegistration apple(
            EventixSecurityProperties.AppleProvider provider) {
        return ClientRegistration.withRegistrationId("apple")
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .clientName("Apple")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "name", "email")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .issuerUri("https://appleid.apple.com")
                .userNameAttributeName("sub")
                .build();
    }
}
