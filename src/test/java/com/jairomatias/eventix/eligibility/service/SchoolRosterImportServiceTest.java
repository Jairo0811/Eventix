package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.jairomatias.eventix.eligibility.repository.PromotionMemberRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolPromotionRepository;
import com.jairomatias.eventix.eligibility.repository.SchoolRosterImportRepository;
import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SchoolRosterImportServiceTest {

    @Mock
    private CsvRosterParser csvRosterParser;
    @Mock
    private NationalIdLookupService nationalIdLookupService;
    @Mock
    private PromotionMemberRepository promotionMemberRepository;
    @Mock
    private SchoolPromotionRepository schoolPromotionRepository;
    @Mock
    private SchoolRosterImportRepository rosterImportRepository;
    @Mock
    private UserRepository userRepository;

    private SchoolRosterImportService service;

    @BeforeEach
    void setUp() {
        service = new SchoolRosterImportService(
                csvRosterParser,
                nationalIdLookupService,
                promotionMemberRepository,
                schoolPromotionRepository,
                rosterImportRepository,
                userRepository);
    }

    @Test
    void rejectsRosterImportFromNonAdministrator() {
        SchoolPromotion promotion = mock(SchoolPromotion.class);
        User organizer = user(RoleName.ORGANIZER);
        byte[] content = validContent();

        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(userRepository.findById(20L)).thenReturn(Optional.of(organizer));

        assertThatThrownBy(() -> service.importCsv(10L, 20L, "Padron oficial", content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo un administrador");

        verify(csvRosterParser, never()).parse(content);
    }

    @Test
    void rejectsRosterImportForInactivePromotion() {
        SchoolPromotion promotion = mock(SchoolPromotion.class);
        User administrator = user(RoleName.ADMINISTRATOR);
        byte[] content = validContent();

        when(schoolPromotionRepository.findById(10L)).thenReturn(Optional.of(promotion));
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));
        when(promotion.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.importCsv(10L, 1L, "Padron oficial", content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deben estar activas");

        verify(csvRosterParser, never()).parse(content);
    }

    private User user(RoleName roleName) {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(roleName);
        return user;
    }

    private byte[] validContent() {
        return "full_name,student_code,national_id,source_reference\n"
                .getBytes(StandardCharsets.UTF_8);
    }
}
