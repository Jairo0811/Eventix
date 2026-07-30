package com.jairomatias.eventix.category.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.category.entity.EventCategory;

public interface EventCategoryRepository
        extends JpaRepository<EventCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<EventCategory> findAllByActiveTrueOrderByNameAsc();

    @Query("""
            SELECT c
            FROM EventCategory c
            WHERE (
                :term = '' OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :term, '%')) OR
                LOWER(COALESCE(c.description, ''))
                    LIKE LOWER(CONCAT('%', :term, '%'))
            )
            AND (:active IS NULL OR c.active = :active)
            """)
    Page<EventCategory> search(
            @Param("term") String term,
            @Param("active") Boolean active,
            Pageable pageable);
}
