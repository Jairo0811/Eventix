package com.jairomatias.eventix.commerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.commerce.dto.RevenueBreakdown;
import com.jairomatias.eventix.commerce.repository.PlatformRevenueRepository;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PlatformRevenueServiceTest {

    @Mock
    private PlatformRevenueRepository revenueRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User actor;

    @Mock
    private Role role;

    @InjectMocks
    private PlatformRevenueService service;

    @Test
    void administratorReceivesGlobalRevenueAndEffectiveRate() {
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("admin", "admin"))
                .thenReturn(Optional.of(actor));
        when(actor.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(RoleName.ADMINISTRATOR);
        when(revenueRepository.sumGrossPaid(null)).thenReturn(new BigDecimal("1000.00"));
        when(revenueRepository.sumPlatformRevenue(null)).thenReturn(new BigDecimal("50.00"));
        when(revenueRepository.sumOrganizerNet(null)).thenReturn(new BigDecimal("950.00"));
        when(revenueRepository.sumRefunded(null)).thenReturn(new BigDecimal("100.00"));

        RevenueBreakdown revenue = service.getRevenue("admin");

        assertThat(revenue.grossRevenue()).isEqualByComparingTo("1000.00");
        assertThat(revenue.platformRevenue()).isEqualByComparingTo("50.00");
        assertThat(revenue.organizerNetRevenue()).isEqualByComparingTo("950.00");
        assertThat(revenue.refundedRevenue()).isEqualByComparingTo("100.00");
        assertThat(revenue.effectivePlatformRate()).isEqualByComparingTo("0.0500");
    }

    @Test
    void organizerRevenueIsScopedToAuthenticatedOrganizer() {
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("organizer", "organizer"))
                .thenReturn(Optional.of(actor));
        when(actor.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(RoleName.ORGANIZER);
        when(actor.getId()).thenReturn(42L);
        when(revenueRepository.sumGrossPaid(42L)).thenReturn(BigDecimal.ZERO);
        when(revenueRepository.sumPlatformRevenue(42L)).thenReturn(BigDecimal.ZERO);
        when(revenueRepository.sumOrganizerNet(42L)).thenReturn(BigDecimal.ZERO);
        when(revenueRepository.sumRefunded(42L)).thenReturn(BigDecimal.ZERO);

        RevenueBreakdown revenue = service.getRevenue("organizer");

        assertThat(revenue.effectivePlatformRate()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(revenueRepository).sumGrossPaid(42L);
        verify(revenueRepository).sumPlatformRevenue(42L);
        verify(revenueRepository).sumOrganizerNet(42L);
        verify(revenueRepository).sumRefunded(42L);
    }
}
