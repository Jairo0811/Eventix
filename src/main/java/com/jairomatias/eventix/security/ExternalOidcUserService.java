package com.jairomatias.eventix.security;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.role.repository.RoleRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class ExternalOidcUserService
        implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public ExternalOidcUserService(
            ExternalIdentityRepository identityRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest request)
            throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);
        String provider = request.getClientRegistration()
                .getRegistrationId()
                .toLowerCase(Locale.ROOT);
        String subject = requiredClaim(oidcUser, "sub");
        String email = requiredClaim(oidcUser, "email")
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!isEmailVerified(oidcUser)) {
            throw authenticationError("external_email_not_verified");
        }

        User user = identityRepository
                .findByProviderAndProviderSubject(provider, subject)
                .map(ExternalIdentity::getUser)
                .orElseGet(() -> linkOrCreateUser(provider, subject, email, oidcUser));

        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw authenticationError("external_identity_email_mismatch");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw authenticationError("external_account_not_active");
        }

        return new ExternalUserPrincipal(user, oidcUser);
    }

    private User linkOrCreateUser(
            String provider,
            String subject,
            String email,
            OidcUser oidcUser) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> createUser(email, oidcUser));
        identityRepository.save(new ExternalIdentity(user, provider, subject));
        return user;
    }

    private User createUser(String email, OidcUser oidcUser) {
        Role role = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> authenticationError("default_user_role_missing"));
        String fullName = oidcUser.getClaimAsString("name");
        if (fullName == null || fullName.isBlank()) {
            fullName = email.substring(0, email.indexOf('@')).replace('.', ' ');
        }
        String[] names = fullName.trim().split("\\s+", 2);
        String firstName = truncate(names[0], 80);
        String lastName = truncate(names.length > 1 ? names[1] : "Usuario", 80);
        String username = uniqueUsername(email.substring(0, email.indexOf('@')));
        String password = passwordEncoder.encode(UUID.randomUUID() + "-external-only");

        User user = new User(
                firstName,
                lastName,
                email,
                username,
                password,
                null,
                role);
        user.setMustChangePassword(false);
        return userRepository.save(user);
    }

    private String uniqueUsername(String candidate) {
        String base = candidate
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "")
                .replaceAll("^[._-]+|[._-]+$", "");
        if (base.isBlank()) {
            base = "usuario";
        }
        base = truncate(base, 50);
        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsernameIgnoreCase(username)) {
            username = base + "-" + suffix++;
        }
        return username;
    }

    private boolean isEmailVerified(OidcUser user) {
        Object claim = user.getClaim("email_verified");
        return Boolean.TRUE.equals(claim)
                || "true".equalsIgnoreCase(String.valueOf(claim));
    }

    private String requiredClaim(OidcUser user, String name) {
        String value = user.getClaimAsString(name);
        if (value == null || value.isBlank()) {
            throw authenticationError("external_" + name + "_missing");
        }
        return value;
    }

    private String truncate(String value, int maximumLength) {
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength);
    }

    private OAuth2AuthenticationException authenticationError(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
