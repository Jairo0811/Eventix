package com.jairomatias.eventix.institution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.eligibility.entity.SchoolInstitutionStatus;
import com.jairomatias.eventix.eligibility.repository.SchoolInstitutionRepository;
import com.jairomatias.eventix.institution.dto.InstitutionRegistrationForm;
import com.jairomatias.eventix.institution.entity.InstitutionMembership;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.repository.InstitutionMembershipRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InstitutionAccountServiceTest {

    @Mock
    private SchoolInstitutionRepository institutionRepository;
    @Mock
    private InstitutionMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InstitutionAuthorizationService authorizationService;

    private InstitutionAccountService service;

    @BeforeEach
    void setUp() {
        service = new InstitutionAccountService(
                institutionRepository,
                membershipRepository,
                userRepository,
                authorizationService);
    }

    @Test
    void registrationCreatesPendingInstitutionAndOwnerMembership() {
        User owner = org.mockito.Mockito.mock(User.class);
        when(owner.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(institutionRepository.existsByCodeIgnoreCase("ABC-001")).thenReturn(false);
        when(institutionRepository.save(any(SchoolInstitution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new InstitutionRegistrationForm("Colegio ABC", "abc-001"), 10L);

        ArgumentCaptor<SchoolInstitution> institutionCaptor = ArgumentCaptor.forClass(SchoolInstitution.class);
        verify(institutionRepository).save(institutionCaptor.capture());
        SchoolInstitution institution = institutionCaptor.getValue();
        assertThat(institution.getStatus()).isEqualTo(SchoolInstitutionStatus.PENDING_VERIFICATION);
        assertThat(institution.isOperational()).isFalse();
        assertThat(institution.getCode()).isEqualTo("ABC-001");

        ArgumentCaptor<InstitutionMembership> membershipCaptor = ArgumentCaptor.forClass(InstitutionMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getRole()).isEqualTo(InstitutionMembershipRole.OWNER);
    }

    @Test
    void duplicateInstitutionCodeIsRejected() {
        User owner = org.mockito.Mockito.mock(User.class);
        when(owner.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(institutionRepository.existsByCodeIgnoreCase("ABC-001")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new InstitutionRegistrationForm("Colegio ABC", "ABC-001"),
                10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya existe");

        verify(institutionRepository, never()).save(any());
        verify(membershipRepository, never()).save(any());
    }
}
