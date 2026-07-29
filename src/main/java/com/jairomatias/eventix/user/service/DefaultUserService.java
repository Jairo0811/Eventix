package com.jairomatias.eventix.user.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.role.repository.RoleRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.shared.exception.DuplicateResourceException;
import com.jairomatias.eventix.shared.exception.ResourceNotFoundException;
import com.jairomatias.eventix.user.dto.ChangePasswordForm;
import com.jairomatias.eventix.user.dto.UserCreateForm;
import com.jairomatias.eventix.user.dto.UserDetailsView;
import com.jairomatias.eventix.user.dto.UserListItem;
import com.jairomatias.eventix.user.dto.UserUpdateForm;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.mapper.UserMapper;
import com.jairomatias.eventix.user.repository.UserRepository;

@Service
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    public DefaultUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            TemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<UserListItem> findAll(String term, UserStatus status, Pageable pageable) {
        String normalizedTerm = term == null ? "" : term.trim();
        return userRepository.search(normalizedTerm, status, pageable)
                .map(userMapper::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserDetailsView findById(Long id) {
        return userMapper.toDetailsView(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserUpdateForm getUpdateForm(Long id) {
        return userMapper.toUpdateForm(findEntity(id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Long create(UserCreateForm form) {
        String email = normalizeEmail(form.getEmail());
        String username = normalizeUsername(form.getUsername());
        validateUniqueFields(email, username, null);

        Role role = findRole(form.getRoleName());
        User user = new User(
                form.getFirstName().trim(),
                form.getLastName().trim(),
                email,
                username,
                passwordEncoder.encode(form.getPassword()),
                normalizeNullable(form.getPhone()),
                role);
        user.setMustChangePassword(true);

        return userRepository.save(user).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void update(Long id, UserUpdateForm form, String authenticatedUsername) {
        User user = findEntity(id);
        String email = normalizeEmail(form.getEmail());
        String username = normalizeUsername(form.getUsername());
        validateUniqueFields(email, username, id);
        validateAdministrativeChange(user, form, authenticatedUsername);

        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(email);
        user.setUsername(username);
        user.setPhone(normalizeNullable(form.getPhone()));
        user.setRole(findRole(form.getRoleName()));
        user.setStatus(form.getStatus());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void activate(Long id) {
        findEntity(id).setStatus(UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deactivate(Long id, String authenticatedUsername) {
        User user = findEntity(id);
        if (matchesIdentity(user, authenticatedUsername)) {
            throw new BusinessRuleException("No puedes desactivar tu propia cuenta.");
        }
        ensureAdministratorRemainsActive(user, user.getRole().getName(), UserStatus.INACTIVE);
        user.setStatus(UserStatus.INACTIVE);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String resetPassword(Long id, String authenticatedUsername) {
        User user = findEntity(id);
        if (matchesIdentity(user, authenticatedUsername)) {
            throw new BusinessRuleException(
                    "Usa la opción «Cambiar contraseña» para actualizar tu propia cuenta.");
        }
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setStatus(UserStatus.ACTIVE);
        return temporaryPassword;
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void changeOwnPassword(String authenticatedUsername, ChangePasswordForm form) {
        User user = findByLogin(authenticatedUsername);

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La contraseña actual no es correcta.");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessRuleException("Las contraseñas no coinciden.");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La nueva contraseña debe ser diferente a la actual.");
        }

        user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
        user.setMustChangePassword(false);
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String login) {
        findByLogin(login).recordLogin(LocalDateTime.now());
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario solicitado no existe."));
    }

    private User findByLogin(String login) {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(login, login)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario autenticado no existe."));
    }

    private Role findRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("El rol seleccionado no existe."));
    }

    private void validateUniqueFields(String email, String username, Long excludedId) {
        boolean duplicatedEmail = excludedId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
        if (duplicatedEmail) {
            throw new DuplicateResourceException("email", "Ya existe un usuario con ese correo.");
        }

        boolean duplicatedUsername = excludedId == null
                ? userRepository.existsByUsernameIgnoreCase(username)
                : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludedId);
        if (duplicatedUsername) {
            throw new DuplicateResourceException("username", "Ya existe ese nombre de usuario.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean matchesIdentity(User user, String login) {
        return user.getEmail().equalsIgnoreCase(login)
                || user.getUsername().equalsIgnoreCase(login);
    }

    private void validateAdministrativeChange(
            User user,
            UserUpdateForm form,
            String authenticatedUsername) {
        boolean editingSelf = matchesIdentity(user, authenticatedUsername);
        if (editingSelf && form.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("No puedes desactivar o bloquear tu propia cuenta.");
        }
        if (editingSelf
                && user.getRole().getName() == RoleName.ADMINISTRATOR
                && form.getRoleName() != RoleName.ADMINISTRATOR) {
            throw new BusinessRuleException("No puedes retirar tu propio rol de administrador.");
        }

        ensureAdministratorRemainsActive(user, form.getRoleName(), form.getStatus());
    }

    private void ensureAdministratorRemainsActive(
            User user,
            RoleName resultingRole,
            UserStatus resultingStatus) {
        boolean removesActiveAdministrator = user.getRole().getName() == RoleName.ADMINISTRATOR
                && user.getStatus() == UserStatus.ACTIVE
                && (resultingRole != RoleName.ADMINISTRATOR || resultingStatus != UserStatus.ACTIVE);

        if (removesActiveAdministrator
                && userRepository.countByRole_NameAndStatus(
                        RoleName.ADMINISTRATOR,
                        UserStatus.ACTIVE) <= 1) {
            throw new BusinessRuleException(
                    "Eventix debe conservar al menos un administrador activo.");
        }
    }
}
