package com.jairomatias.eventix.promotion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.promotion.entity.Coupon;

import jakarta.persistence.LockModeType;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("""
            SELECT c
            FROM Coupon c
            WHERE (
                :term = ''
                OR LOWER(c.code) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(c.description) LIKE LOWER(CONCAT('%', :term, '%'))
            )
            AND (:active IS NULL OR c.active = :active)
            """)
    Page<Coupon> search(
            @Param("term") String term,
            @Param("active") Boolean active,
            Pageable pageable);

    @EntityGraph(attributePaths = "applicableEvents")
    @Query("SELECT c FROM Coupon c WHERE c.id = :id")
    Optional<Coupon> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "applicableEvents")
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);
}
