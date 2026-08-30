package com.jairomatias.eventix.institution.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.institution.entity.InstitutionMembership;

public interface InstitutionMembershipRepository extends JpaRepository<InstitutionMembership, Long> {

    @EntityGraph(attributePaths = {"institution", "user"})
    List<InstitutionMembership> findAllByUser_IdOrderByInstitution_NameAsc(Long userId);

    @EntityGraph(attributePaths = {"institution", "user"})
    Optional<InstitutionMembership> findByInstitution_IdAndUser_Id(Long institutionId, Long userId);

    @EntityGraph(attributePaths = {"institution", "user"})
    List<InstitutionMembership> findAllByInstitution_IdOrderByUser_LastNameAscUser_FirstNameAsc(Long institutionId);

    boolean existsByInstitution_IdAndUser_Id(Long institutionId, Long userId);
}
