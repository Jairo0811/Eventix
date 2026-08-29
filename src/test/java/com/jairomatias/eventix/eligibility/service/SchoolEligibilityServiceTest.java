package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.entity.PromotionMember;
import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationAttemptRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityVerificationRepository;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SchoolEligibilityServiceTest {

    @Mock
    private PromotionMemberRepository promotionMemberRepository;
    @Mock
    private NationalIdLookupService nationalIdLookupService;
    @Mock
    private CitizenIdentityProvider citizenIdentityProvider;
    @Mock
    private EligibilityVerificationRepository verificationRepository;
    @Mock
    private EligibilityVerificationAttemptRepository attemptRepository;
    @Mock
    private SchoolPromotionRepository schoolPromotionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolPromotionMembershipSyncService membershipSyncService;

    private SchoolEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new SchoolEligibilityService(
                promotionMemberRepository,
                nationalIdLookupService,
                citizenIdentityProvider,
                new PersonNameNormalizer(),
                verificationRepository,
                attemptRepository,
                schoolPromotionRepository,
                userRepository,
                membershipSyncService);
    }

    @Test
    void verifiesUsingOfficialIdentityNameEvenWhenEventixProfileNameDiffers() {
        User user = user("Cuenta", "Distinta");
        SchoolPromotion promotion = promotion();
        PromotionMember member = member(promotion, "Ana Pérez Gómez");
        stubCommon(user, promotion);
        when(citizenIdentityProvider.lookupByNationalId("00112345678"))
                .thenReturn(CitizenIdentityLookupResult.found("ANA PEREZ GOMEZ"));
        when(promotionMemberRepository.findAllByPromotion_IdAndActiveTrueOrderByFullNameAsc(10L))
                .thenReturn(List.of(member));
        when(verificationRepository.findByUser_IdAndPromotionMember_Id(1L, null))
                .thenReturn(Optional.empty());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isTrue();
        assertThat(result.status()).isEqualTo("VERIFIED");
        verify(verificationRepository).save(any());
        verify(attemptRepository).save(any());
        verify(membershipSyncService).syncVerifiedUser(1L, 10L);
    }

    @Test
    void returnsNotFoundWhenOfficialIdentityNameIsAbsentFromRoster() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        stubCommon(user, promotion);
        when(citizenIdentityProvider.lookupByNationalId("00112345678"))
                .thenReturn(CitizenIdentityLookupResult.found("Ana Pérez Gómez"));
        when(promotionMemberRepository.findAllByPromotion_IdAndActiveTrueOrderByFullNameAsc(10L))
                .thenReturn(List.of(member(promotion, "Luis Ramírez Santos")));

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isFalse();
        assertThat(result.status()).isEqualTo("NOT_FOUND");
        verify(attemptRepository).save(any());
        verify(verificationRepository, never()).save(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    @Test
    void sendsDuplicateOfficialNameMatchesToManualReview() {
        User user = user("Cualquier", "Nombre");
        SchoolPromotion promotion = promotion();
        PromotionMember first = member(promotion, "Ana Pérez Gómez");
        PromotionMember second = member(promotion, "ANA PEREZ GOMEZ");
        stubCommon(user, promotion);
        when(citizenIdentityProvider.lookupByNationalId("00112345678"))
                .thenReturn(CitizenIdentityLookupResult.found("Ana Perez Gomez"));
        when(promotionMemberRepository.findAllByPromotion_IdAndActiveTrueOrderByFullNameAsc(10L))
                .thenReturn(List.of(first, second));
        when(verificationRepository.findByUser_IdAndPromotionMember_Id(1L, null))
                .thenReturn(Optional.empty());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isFalse();
        assertThat(result.status()).isEqualTo("MANUAL_REVIEW");
        verify(verificationRepository).save(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    @Test
    void failsClosedWhenIdentityProviderIsUnavailable() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        stubCommon(user, promotion);
        when(citizenIdentityProvider.lookupByNationalId("00112345678"))
                .thenReturn(CitizenIdentityLookupResult.unavailable());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.status()).isEqualTo("IDENTITY_PROVIDER_UNAVAILABLE");
        assertThat(result.eligible()).isFalse();
        verify(attemptRepository).save(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    @Test
    void recordsIdentityNotFoundWithoutConsultingRoster() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        stubCommon(user, promotion);
        when(citizenIdentityProvider.lookupByNationalId("00112345678"))
                .thenReturn(CitizenIdentityLookupResult.notFound());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.status()).isEqualTo("IDENTITY_NOT_FOUND");
        assertThat(result.eligible()).isFalse();
        verify(attemptRepository).save(any());
        verify(promotionMemberRepository, never())
                .findAllByPromotion_IdAndActiveTrueOrderByFullNameAsc(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    private void stubCommon(User user, SchoolPromotion promotion) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(nationalIdLookupService.lookupKey("00112345678")).thenReturn("lookup");
        when(nationalIdLookupService.last4("00112345678")).thenReturn("5678");
    }

    private User user(String firstName, String lastName) {
        return new User(firstName, lastName, "ana@example.com", "ana", "hash", null, null);
    }

    private SchoolPromotion promotion() {
        SchoolInstitution institution = new SchoolInstitution("Colegio Demo", "DEMO");
        return new SchoolPromotion(institution, "Promoción 2017", 2017);
    }

    private PromotionMember member(SchoolPromotion promotion, String name) {
        return new PromotionMember(promotion, name, "A-2017-01", "Acta oficial");
    }
}
