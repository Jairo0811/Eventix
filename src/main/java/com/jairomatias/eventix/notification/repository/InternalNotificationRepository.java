package com.jairomatias.eventix.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.notification.entity.InternalNotification;

public interface InternalNotificationRepository
        extends JpaRepository<InternalNotification, Long> {

    Page<InternalNotification> findAllByRecipient_IdOrderByCreatedAtDesc(
            Long recipientId,
            Pageable pageable);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    Optional<InternalNotification> findByIdAndRecipient_Id(
            Long id,
            Long recipientId);
}
