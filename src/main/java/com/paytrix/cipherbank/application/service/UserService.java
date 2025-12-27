package com.paytrix.cipherbank.application.service;

import com.paytrix.cipherbank.application.port.in.UserUseCase;
import com.paytrix.cipherbank.application.port.out.security.UserRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements UserUseCase {

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepositoryPort.findByUsername(username);
    }
}
