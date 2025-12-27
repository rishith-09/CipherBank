package com.paytrix.cipherbank.application.port.out.security;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);
    User save(User user);
    boolean existsByUsername(String username);
}
