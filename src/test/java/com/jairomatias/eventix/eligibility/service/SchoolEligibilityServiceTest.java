package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private EligibilityVerificationRepository verificationRepository;
    @Mock
    private EligibilityVerificationAttemptRepository attemptRepository;
    @Mock
    private SchoolPromotionRepository schoolPromotionRepository;
    @Mock
    private UserRepository userRepository;

    private SchoolEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new SchoolEligibilityService(
                promotionMemberRepository,
                nationalIdLookupService,
                verificationRepository,
                attemptRepository,
                schoolPromotionRepository,
                userRepository);
    }

    @Test
    void verifiesWhenNationalIdAndNameMatchAuthorizedRoster() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        PromotionMember member = member(promotion, "Ana Pérez Gómez");
        stubLookup(user, promotion, member);

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isTrue();
        assertThat(result.status()).isEqualTo("VERIFIED");
        verify(verificationRepository).save(any());
        verify(attemptRepository).save(any());
    }

    @Test
    void requiresManualReviewWhenNationalIdMatchesButNameDiffers() {
        User user = user("Ana", "Pérez Gómez");
        SchoolPromotion promotion = promotion();
        PromotionMember member = member(promotion, "Ana María Pérez Gómez");
        stubLookup(user, promotion, member);

        var result = service.verifyAndPersist(1L, 10L, "00112345678");

        assertThat(result.eligible()).isFalse();
        assertThat(result.status()).isEqualTo("MANUAL_REVIEW");
        verify(verificationRepository).save(any());
        verify(attemptRepository).save(any());
    }

    private void stubLookup(
            User user,
            SchoolPromotion promotion,
            PromotionMember member) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(nationalIdLookupService.lookupKey("00112345678")).thenReturn("lookup");
        when(nationalIdLookupService.last4("00112345678")).thenReturn("5678");
        when(promotionMemberRepository
                .findByPromotion_IdAndNationalIdLookupAndActiveTrue(10L, "lookup"))
                .thenReturn(Optional.of(member));
        when(verificationRepository.findByUser_IdAndPromotionMember_Id(1L, null))
                .thenReturn(Optional.empty());
    }

    private User user(String firstName, String lastName) {
        return new User(
                firstName,
                lastName,
                "ana@example.com",
                "ana",
                "hash",
                null,
                null);
    }

    private SchoolPromotion promotion() {
        SchoolInstitution institution = new SchoolInstitution("Colegio Demo", "DEMO");
        return new SchoolPromotion(institution, "Promoción 2017", 2017);
    }

    private PromotionMember member(SchoolPromotion promotion, String name) {
        return new PromotionMember(
                promotion,
                name,
                "A-2017-01",
                "lookup",
                "5678",
                "Acta oficial");
    }
}
