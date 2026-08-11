package com.jairomatias.eventix.notification.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.notification.entity.EventReminderDelivery;
import com.jairomatias.eventix.notification.entity.ReminderDeliveryStatus;

import jakarta.persistence.LockModeType;

public interface EventReminderDeliveryRepository
        extends JpaRepository<EventReminderDelivery, Long> {

    @Query("""
            SELECT delivery.id
            FROM EventReminderDelivery delivery
            WHERE delivery.status IN :statuses
              AND (delivery.status = :pendingStatus
                   OR delivery.nextAttemptAt <= :now)
            ORDER BY delivery.createdAt ASC
            """)
    List<Long> findDueIds(
            @Param("statuses") Collection<ReminderDeliveryStatus> statuses,
            @Param("pendingStatus") ReminderDeliveryStatus pendingStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "event")
    @Query("""
            SELECT delivery
            FROM EventReminderDelivery delivery
            WHERE delivery.id = :id
            """)
    Optional<EventReminderDelivery> findDetailedByIdForUpdate(
            @Param("id") Long id);
}
