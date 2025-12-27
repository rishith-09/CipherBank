package com.paytrix.cipherbank.infrastructure.adapter.out.persistence.adapter.security;

import com.paytrix.cipherbank.application.port.out.security.UserRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.repository.security.JpaUserRepository;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    @Autowired
    private JpaUserRepository jpa;

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username);
    }

    @Override
    public User save(User user) {
        return jpa.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }
}
