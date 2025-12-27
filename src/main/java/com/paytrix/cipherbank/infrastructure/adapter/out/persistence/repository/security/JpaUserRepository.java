package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
