package com.jairomatias.eventix.institution.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.institution.entity.InstitutionMembership;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipStatus;
import com.jairomatias.eventix.institution.repository.InstitutionMembershipRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InstitutionAuthorizationServiceTest {

    @Mock
    private InstitutionMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;

    private InstitutionAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new InstitutionAuthorizationService(membershipRepository, userRepository);
    }

    @Test
    void crossTenantUserCannotAccessAnotherInstitution() {
        SchoolInstitution institution = mock(SchoolInstitution.class);
        User actor = user(RoleName.USER);
        when(institution.getId()).thenReturn(99L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(actor));
        when(membershipRepository.findByInstitution_IdAndUser_Id(99L, 7L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireInstitutionRole(
                institution,
                7L,
                InstitutionMembershipRole.ROSTER_MANAGER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No perteneces");
    }

    @Test
    void pendingInstitutionCannotExecuteOperationalActions() {
        SchoolInstitution institution = mock(SchoolInstitution.class);
        InstitutionMembership membership = mock(InstitutionMembership.class);
        User actor = user(RoleName.USER);
        when(institution.getId()).thenReturn(20L);
        when(institution.isOperational()).thenReturn(false);
        when(userRepository.findById(5L)).thenReturn(Optional.of(actor));
        when(membershipRepository.findByInstitution_IdAndUser_Id(20L, 5L))
                .thenReturn(Optional.of(membership));
        when(membership.getStatus()).thenReturn(InstitutionMembershipStatus.ACTIVE);
        when(membership.getRole()).thenReturn(InstitutionMembershipRole.OWNER);

        assertThatThrownBy(() -> service.requireOperationalRole(
                institution,
                5L,
                InstitutionMembershipRole.OWNER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aprobado y activo");
    }

    private User user(RoleName roleName) {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(roleName);
        return user;
    }
}
