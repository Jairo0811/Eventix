package com.jairomatias.eventix.eligibility.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.eligibility.entity.SchoolInstitution;

public interface SchoolInstitutionRepository extends JpaRepository<SchoolInstitution, Long> {

    List<SchoolInstitution> findAllByOrderByNameAsc();

    List<SchoolInstitution> findAllByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
