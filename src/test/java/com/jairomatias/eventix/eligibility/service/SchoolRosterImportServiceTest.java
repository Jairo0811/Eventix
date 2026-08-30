package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolRosterImportRepository;
import com.jairomatias.eventix.institution.entity.InstitutionMembershipRole;
import com.jairomatias.eventix.institution.service.InstitutionAuthorizationService;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SchoolRosterImportServiceTest {

    @Mock
    private CsvRosterParser csvRosterParser;
    @Mock
    private PromotionMemberRepository promotionMemberRepository;
    @Mock
    private SchoolPromotionRepository schoolPromotionRepository;
    @Mock
    private SchoolRosterImportRepository rosterImportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InstitutionAuthorizationService authorizationService;

    private SchoolRosterImportService service;

    @BeforeEach
    void setUp() {
        service = new SchoolRosterImportService(
                csvRosterParser,
                new PersonNameNormalizer(),
                promotionMemberRepository,
                schoolPromotionRepository,
                rosterImportRepository,
                userRepository,
                authorizationService);
    }

    @Test
    void rejectsRosterImportWithoutInstitutionPermission() {
        SchoolPromotion promotion = mock(SchoolPromotion.class);
        SchoolInstitution institution = mock(SchoolInstitution.class);
        User actor = mock(User.class);
        byte[] content = validContent();

        when(promotion.getInstitution()).thenReturn(institution);
        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(userRepository.findById(20L)).thenReturn(Optional.of(actor));
        doThrow(new BusinessRuleException("Sin permiso institucional."))
                .when(authorizationService)
                .requireOperationalRole(
                        eq(institution),
                        eq(20L),
                        any(InstitutionMembershipRole[].class));

        assertThatThrownBy(() -> service.importCsv(10L, 20L, "Padron oficial", content))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Sin permiso");

        verify(csvRosterParser, never()).parse(content);
    }

    @Test
    void rejectsRosterImportForInactivePromotion() {
        SchoolPromotion promotion = mock(SchoolPromotion.class);
        User actor = mock(User.class);
        byte[] content = validContent();

        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(promotion.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.importCsv(10L, 1L, "Padron oficial", content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("promoción debe estar activa");

        verify(csvRosterParser, never()).parse(content);
    }

    private byte[] validContent() {
        return "full_name,student_code,source_reference\n"
                .getBytes(StandardCharsets.UTF_8);
    }
}
