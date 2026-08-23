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

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationship;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationshipStatus;
import com.jairomatias.eventix.eligibility.entity.EligibilityRelationshipType;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityRelationshipRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FamilyEligibilityServiceTest {

    @Mock
    private EligibilityGroupRepository groupRepository;
    @Mock
    private EligibilityMembershipRepository membershipRepository;
    @Mock
    private EligibilityRelationshipRepository relationshipRepository;
    @Mock
    private UserRepository userRepository;

    private FamilyEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new FamilyEligibilityService(
                groupRepository,
                membershipRepository,
                relationshipRepository,
                userRepository);
    }

    @Test
    void rejectsRequestWhenSponsorHasNoVerifiedPrimaryEligibility() {
        Event event = mock(Event.class);
        EligibilityGroup group = mock(EligibilityGroup.class);
        User sponsor = user(20L);
        User related = user(30L);

        when(group.isActive()).thenReturn(true);
        when(group.getGroupType()).thenReturn(EligibilityGroupType.FAMILY);
        when(group.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(100L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(20L)).thenReturn(Optional.of(sponsor));
        when(userRepository.findById(30L)).thenReturn(Optional.of(related));
        when(membershipRepository
                .existsByGroup_Event_IdAndUser_IdAndStatusAndActiveTrueAndGroup_GroupTypeNot(
                        100L, 20L, EligibilityMembershipStatus.VERIFIED, EligibilityGroupType.FAMILY))
                .thenReturn(false);

        assertThatThrownBy(() -> service.requestFamilyLink(
                10L, 20L, 30L, EligibilityRelationshipType.SIBLING, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("patrocinador");

        verify(relationshipRepository, never()).save(any());
    }

    @Test
    void rejectsApprovalWhenSponsorReachedFamilyLimit() {
        Event event = mock(Event.class);
        EligibilityGroup group = mock(EligibilityGroup.class);
        User sponsor = user(20L);
        User reviewer = administrator();
        EligibilityRelationship relationship = mock(EligibilityRelationship.class);

        when(group.isActive()).thenReturn(true);
        when(group.getGroupType()).thenReturn(EligibilityGroupType.FAMILY);
        when(group.getId()).thenReturn(10L);
        when(group.getEvent()).thenReturn(event);
        when(event.getId()).thenReturn(100L);
        when(group.getMaxRelatedPeople()).thenReturn(2);
        when(relationship.getGroup()).thenReturn(group);
        when(relationship.getSponsorUser()).thenReturn(sponsor);
        when(relationshipRepository.findDetailedByIdForUpdate(50L))
                .thenReturn(Optional.of(relationship));
        when(groupRepository.findDetailedByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(40L)).thenReturn(Optional.of(reviewer));
        when(membershipRepository
                .existsByGroup_Event_IdAndUser_IdAndStatusAndActiveTrueAndGroup_GroupTypeNot(
                        100L, 20L, EligibilityMembershipStatus.VERIFIED, EligibilityGroupType.FAMILY))
                .thenReturn(true);
        when(relationshipRepository.countByGroup_IdAndSponsorUser_IdAndStatus(
                10L, 20L, EligibilityRelationshipStatus.APPROVED)).thenReturn(2L);

        assertThatThrownBy(() -> service.approve(50L, 40L, "Validación documental completa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("límite de 2");

        verify(relationship, never()).approve(any(), any(), any());
    }

    private User user(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private User administrator() {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(RoleName.ADMINISTRATOR);
        when(user.getRole()).thenReturn(role);
        return user;
    }
}
