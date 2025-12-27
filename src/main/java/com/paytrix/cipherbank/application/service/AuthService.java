package com.paytrix.cipherbank.application.service;

import com.paytrix.cipherbank.application.port.in.AuthUseCase;
import com.paytrix.cipherbank.application.port.out.security.RoleRepositoryPort;
import com.paytrix.cipherbank.application.port.out.security.UserRepositoryPort;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.Role;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;
import com.paytrix.cipherbank.infrastructure.exception.DuplicateResourceException;
import com.paytrix.cipherbank.infrastructure.exception.ResourceNotFoundException;
import com.paytrix.cipherbank.infrastructure.security.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private RoleRepositoryPort roleRepositoryPort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public String login(String username, String password) {
        log.info("Login attempt for username: {}", username);

        var opt = userRepositoryPort.findByUsername(username);

        if (opt.isEmpty()) {
            log.warn("Login failed: User '{}' not found", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = opt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed: Invalid password for user '{}'", username);
            throw new BadCredentialsException("Invalid username or password");
        }

        log.info("Login successful for user: {}", username);
        return jwtTokenUtil.generateTokenFromUser(user);
    }

    @Override
    public User register(User user) {
        return registerUserWithRoleIds(user.getUsername(), user.getPassword(), null);
    }

    @Override
    public User registerUserWithRoleIds(String username, String rawPassword, List<Long> roleIds) {
        log.info("Registration attempt for username: {}", username);

        // Check if username already exists
        if (userRepositoryPort.existsByUsername(username)) {
            log.warn("Registration failed: Username '{}' already exists", username);
            throw new DuplicateResourceException("User", username);
        }

        // Validate password
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            log.error("Registration failed: Password is required");
            throw new IllegalArgumentException("Password is required");
        }

        if (rawPassword.length() < 6) {
            log.error("Registration failed: Password too short");
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        // Create user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        Set<Role> roles = new HashSet<>();

        if (roleIds != null && !roleIds.isEmpty()) {
            // Fetch roles by IDs
            for (Long id : roleIds) {
                Role role = roleRepositoryPort.findById(id)
                        .orElseThrow(() -> {
                            log.error("Registration failed: Invalid role ID: {}", id);
                            return new ResourceNotFoundException("Role", id.toString());
                        });
                roles.add(role);
            }
        } else {
            // Default to ROLE_USER
            Role roleUser = roleRepositoryPort.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        log.info("ROLE_USER not found, creating it");
                        return roleRepositoryPort.save(new Role(null, "ROLE_USER"));
                    });
            roles.add(roleUser);
        }

        user.setRoles(roles);

        User savedUser = userRepositoryPort.save(user);
        log.info("User '{}' registered successfully with roles: {}",
                username,
                roles.stream().map(Role::getName).toList());

        return savedUser;
    }
}
