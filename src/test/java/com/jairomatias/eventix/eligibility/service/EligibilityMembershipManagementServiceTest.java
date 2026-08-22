package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.dto.EligibilityMembershipForm;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EligibilityMembershipManagementServiceTest {

    @Mock
    private EligibilityGroupRepository groupRepository;
    @Mock
    private EligibilityMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;

    private EligibilityMembershipManagementService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityMembershipManagementService(
                groupRepository,
                membershipRepository,
                userRepository);
    }

    @Test
    void familyGroupRejectsDirectVerifiedMembership() {
        EligibilityGroup group = mock(EligibilityGroup.class);
        Event event = mock(Event.class);
        User administrator = administrator();

        when(groupRepository.findDetailedByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(group.getEvent()).thenReturn(event);
        when(group.getGroupType()).thenReturn(EligibilityGroupType.FAMILY);
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));

        assertThatThrownBy(() -> service.addVerified(
                10L,
                new EligibilityMembershipForm("family@example.com"),
                1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("flujo de verificación específico");

        verify(membershipRepository, never()).save(any());
    }

    private User administrator() {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(RoleName.ADMINISTRATOR);
        return user;
    }
}
