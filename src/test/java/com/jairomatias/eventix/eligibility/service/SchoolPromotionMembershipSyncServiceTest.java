package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroupType;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembership;
import com.jairomatias.eventix.eligibility.entity.EligibilityMembershipStatus;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityMembershipRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SchoolPromotionMembershipSyncServiceTest {

    @Mock
    private EligibilityGroupRepository groupRepository;
    @Mock
    private EligibilityMembershipRepository membershipRepository;
    @Mock
    private EligibilityVerificationRepository verificationRepository;
    @Mock
    private UserRepository userRepository;

    private SchoolPromotionMembershipSyncService service;

    @BeforeEach
    void setUp() {
        service = new SchoolPromotionMembershipSyncService(
                groupRepository, membershipRepository, verificationRepository, userRepository);
    }

    @Test
    void verifiedSchoolUserGetsVerifiedMembershipInEveryLinkedActiveGroup() {
        User user = mock(User.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        EligibilityGroup group = mock(EligibilityGroup.class);
        when(group.getId()).thenReturn(40L);
        when(groupRepository.findAllBySchoolPromotion_IdAndGroupTypeAndActiveTrue(
                10L, EligibilityGroupType.PROMOTION_MEMBER)).thenReturn(List.of(group));
        when(membershipRepository.findByGroup_IdAndUser_Id(40L, 7L)).thenReturn(Optional.empty());
        when(user.getId()).thenReturn(7L);

        service.syncVerifiedUser(7L, 10L);

        ArgumentCaptor<EligibilityMembership> captor = ArgumentCaptor.forClass(EligibilityMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EligibilityMembershipStatus.VERIFIED);
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void revocationRemovesBenefitsFromEveryGroupLinkedToPromotion() {
        EligibilityMembership membership = mock(EligibilityMembership.class);
        when(membershipRepository.findAllByGroup_SchoolPromotion_IdAndUser_Id(10L, 7L))
                .thenReturn(List.of(membership));

        service.revokeForPromotion(7L, 10L);

        verify(membership).revoke();
        verify(membershipRepository).save(membership);
    }
}
