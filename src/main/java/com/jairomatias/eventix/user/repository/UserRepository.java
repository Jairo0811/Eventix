package com.jairomatias.eventix.user.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jairomatias.eventix.user.entity.User;
import com.jairomatias.eventix.user.entity.UserStatus;
import com.jairomatias.eventix.role.entity.RoleName;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    long countByStatus(UserStatus status);

    long countByRole_NameAndStatus(RoleName roleName, UserStatus status);

    List<User> findAllByRole_NameAndStatusOrderByLastNameAscFirstNameAsc(
            RoleName roleName,
            UserStatus status);

    @Query(
            value = """
                    SELECT u
                    FROM User u
                    JOIN FETCH u.role r
                    WHERE (
                        :term = '' OR
                        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR u.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(u)
                    FROM User u
                    WHERE (
                        :term = '' OR
                        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')) OR
                        LOWER(u.username) LIKE LOWER(CONCAT('%', :term, '%'))
                    )
                    AND (:status IS NULL OR u.status = :status)
                    """)
    Page<User> search(
            @Param("term") String term,
            @Param("status") UserStatus status,
            Pageable pageable);
}
