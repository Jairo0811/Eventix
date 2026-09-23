package com.jairomatias.eventix.security;

import java.util.Map;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.jairomatias.eventix.user.entity.User;

public final class ExternalUserPrincipal extends UserPrincipal implements OidcUser {

    private final OidcUser delegate;

    public ExternalUserPrincipal(User user, OidcUser delegate) {
        super(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().getName().name(),
                user.getStatus(),
                user.isMustChangePassword());
        this.delegate = delegate;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public String getName() {
        return getUsername();
    }
}
