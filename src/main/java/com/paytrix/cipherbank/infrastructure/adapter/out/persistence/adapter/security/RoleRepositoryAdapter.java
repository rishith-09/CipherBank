package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.adapter.security;

import com.paytrix.cipherbank.application.port.out.security.RoleRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security.JpaRoleRepository;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    @Autowired
    private JpaRoleRepository jpa;

    @Override
    public Optional<Role> findByName(String name) {
        return jpa.findByName(name);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Role save(Role role) {
        return jpa.save(role);
    }
}
