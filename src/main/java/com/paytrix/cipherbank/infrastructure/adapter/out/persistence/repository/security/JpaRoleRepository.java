package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
