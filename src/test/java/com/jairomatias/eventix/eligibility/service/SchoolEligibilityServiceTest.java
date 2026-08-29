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
import com.jairomatias.eventix.eligibility.identity.CitizenIdentity;
import com.jairomatias.eventix.eligibility.identity.CitizenIdentityProvider;
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
    private PersonNameNormalizer nameNormalizer;
    @Mock
    private CitizenIdentityProvider identityProvider;
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
                nameNormalizer,
                identityProvider,
                verificationRepository,
                attemptRepository,
                schoolPromotionRepository,
                userRepository,
                membershipSyncService);
    }

    @Test
    void verifiesWhenOfficialIdentityNameExistsInRosterRegardlessOfEventixProfileName() {
        User user = user("Nombre", "De Cuenta");
        SchoolPromotion promotion = promotion();
        PromotionMember member = member(promotion, "Ana Pérez Gómez", "ANA PEREZ GOMEZ");
        stubIdentityLookup(user, promotion, "Ana Perez Gomez");
        when(nameNormalizer.normalize("Ana Perez Gomez")).thenReturn("ANA PEREZ GOMEZ");
        when(promotionMemberRepository.findAllByPromotion_IdAndNormalizedFullNameAndActiveTrue(
                10L, "ANA PEREZ GOMEZ")).thenReturn(List.of(member));
        when(verificationRepository.findByUser_IdAndPromotionMember_Id(1L, null))
                .thenReturn(Optional.empty());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isTrue();
        assertThat(result.status()).isEqualTo("VERIFIED");
        assertThat(result.memberName()).isEqualTo("Ana Pérez Gómez");
        verify(verificationRepository).save(any());
        verify(attemptRepository).save(any());
        verify(membershipSyncService).syncVerifiedUser(1L, 10L);
    }

    @Test
    void deniesWhenOfficialIdentityNameDoesNotExistInRoster() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        stubIdentityLookup(user, promotion, "Ana Perez Gomez");
        when(nameNormalizer.normalize("Ana Perez Gomez")).thenReturn("ANA PEREZ GOMEZ");
        when(promotionMemberRepository.findAllByPromotion_IdAndNormalizedFullNameAndActiveTrue(
                10L, "ANA PEREZ GOMEZ")).thenReturn(List.of());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isFalse();
        assertThat(result.status()).isEqualTo("NOT_FOUND");
        verify(verificationRepository, never()).save(any());
        verify(attemptRepository).save(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    @Test
    void reportsIdentityNotFoundWhenProviderCannotResolveNationalId() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        stubNationalIdProtection();
        when(identityProvider.findByNationalId("00112345678")).thenReturn(Optional.empty());

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.status()).isEqualTo("IDENTITY_NOT_FOUND");
        verify(attemptRepository).save(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    @Test
    void refusesAutomaticVerificationWhenOfficialNameAppearsMoreThanOnce() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        PromotionMember first = member(promotion, "Ana Perez Gomez", "ANA PEREZ GOMEZ");
        PromotionMember second = member(promotion, "Ana Pérez Gómez", "ANA PEREZ GOMEZ");
        stubIdentityLookup(user, promotion, "Ana Perez Gomez");
        when(nameNormalizer.normalize("Ana Perez Gomez")).thenReturn("ANA PEREZ GOMEZ");
        when(promotionMemberRepository.findAllByPromotion_IdAndNormalizedFullNameAndActiveTrue(
                10L, "ANA PEREZ GOMEZ")).thenReturn(List.of(first, second));

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.status()).isEqualTo("AMBIGUOUS_MATCH");
        verify(verificationRepository, never()).save(any());
        verify(attemptRepository).save(any());
        verify(membershipSyncService, never()).syncVerifiedUser(any(), any());
    }

    private void stubIdentityLookup(User user, SchoolPromotion promotion, String officialName) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        stubNationalIdProtection();
        when(identityProvider.findByNationalId("00112345678"))
                .thenReturn(Optional.of(new CitizenIdentity(officialName)));
    }

    private void stubNationalIdProtection() {
        when(nationalIdLookupService.normalizeNationalId("00112345678")).thenReturn("00112345678");
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

    private PromotionMember member(SchoolPromotion promotion, String name, String normalizedName) {
        return new PromotionMember(
                promotion,
                name,
                normalizedName,
                "A-2017-01",
                "Acta oficial");
    }
}
