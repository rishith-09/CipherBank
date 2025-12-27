package com.paytrix.cipherbank.application.port.out.security;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.Role;

import java.util.Optional;

public interface RoleRepositoryPort {

    Optional<Role> findByName(String name);
    Optional<Role> findById(Long id);
    Role save(Role role);
}
