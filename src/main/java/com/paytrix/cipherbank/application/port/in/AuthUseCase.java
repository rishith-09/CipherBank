package com.paytrix.cipherbank.application.port.in;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;

import java.util.List;

public interface AuthUseCase {

    String login(String username, String password);
    User register(User user);
    User registerUserWithRoleIds(String username, String rawPassword, List<Long> roleIds);
}
