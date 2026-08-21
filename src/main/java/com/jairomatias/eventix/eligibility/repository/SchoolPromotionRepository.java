package com.jairomatias.eventix.eligibility.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.SchoolPromotion;

public interface SchoolPromotionRepository extends JpaRepository<SchoolPromotion, Long> {
}
