package com.jairomatias.eventix.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.ticket.entity.AppleWalletRegistration;

public interface AppleWalletRegistrationRepository
        extends JpaRepository<AppleWalletRegistration, Long> {

    Optional<AppleWalletRegistration>
            findByDeviceLibraryIdentifierAndTicket_Id(
                    String deviceLibraryIdentifier,
                    Long ticketId);

    void deleteByDeviceLibraryIdentifierAndTicket_Id(
            String deviceLibraryIdentifier,
            Long ticketId);

    @EntityGraph(attributePaths = {"ticket", "ticket.event"})
    List<AppleWalletRegistration>
            findAllByDeviceLibraryIdentifierAndTicket_PassUpdatedAtAfter(
                    String deviceLibraryIdentifier,
                    LocalDateTime updatedAfter);

    List<AppleWalletRegistration> findAllByTicket_Id(Long ticketId);
}
