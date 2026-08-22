package com.jairomatias.eventix.eligibility.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jairomatias.eventix.eligibility.dto.EligibilityBenefitForm;
import com.jairomatias.eventix.eligibility.entity.EligibilityBenefitType;
import com.jairomatias.eventix.eligibility.entity.EligibilityGroup;
import com.jairomatias.eventix.eligibility.repository.EligibilityBenefitRepository;
import com.jairomatias.eventix.eligibility.repository.EligibilityGroupRepository;
import com.jairomatias.eventix.event.entity.Event;
import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;
import com.jairomatias.eventix.sale.repository.TicketTypeRepository;
import com.jairomatias.eventix.shared.exception.BusinessRuleException;
import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class EligibilityBenefitManagementServiceTest {

    @Mock
    private EligibilityGroupRepository groupRepository;
    @Mock
    private EligibilityBenefitRepository benefitRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private UserRepository userRepository;

    private EligibilityBenefitManagementService service;

    @BeforeEach
    void setUp() {
        service = new EligibilityBenefitManagementService(
                groupRepository,
                benefitRepository,
                ticketTypeRepository,
                userRepository);
    }

    @Test
    void percentageDiscountCannotExceedOneHundred() {
        EligibilityGroup group = mock(EligibilityGroup.class);
        Event event = mock(Event.class);
        User administrator = administrator();

        when(groupRepository.findDetailedByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(group.getEvent()).thenReturn(event);
        when(group.isActive()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(administrator));

        EligibilityBenefitForm form = new EligibilityBenefitForm(
                EligibilityBenefitType.PERCENTAGE_DISCOUNT,
                new BigDecimal("120.00"),
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.create(10L, form, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("100%");

        verify(benefitRepository, never()).save(any());
    }

    private User administrator() {
        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getRole()).thenReturn(role);
        when(role.getName()).thenReturn(RoleName.ADMINISTRATOR);
        return user;
    }
}
