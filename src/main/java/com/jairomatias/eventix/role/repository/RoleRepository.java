package com.jairomatias.eventix.role.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jairomatias.eventix.role.entity.Role;
import com.jairomatias.eventix.role.entity.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}

