package com.paytrix.cipherbank.infrastructure.adapter.in.controller;

import com.paytrix.cipherbank.application.port.in.AuthUseCase;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.Role;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;
import com.paytrix.cipherbank.domain.model.UserRegistrationRequest;
import com.paytrix.cipherbank.domain.model.UserResponse;
import com.paytrix.cipherbank.application.port.out.security.UserRepositoryPort;
import com.paytrix.cipherbank.domain.model.ChangePasswordRequest;
import com.paytrix.cipherbank.infrastructure.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// DTO Records
record LoginRequest(String username, String password) {}
record AuthResponse(String token) {}

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthUseCase authService;

    @Autowired
    private UserRepositoryPort userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login request for username: {}", request.username());

        // Validate request
        if (request.username() == null || request.username().trim().isEmpty()) {
            log.warn("Login failed: Username is required");
            throw new IllegalArgumentException("Username is required");
        }

        if (request.password() == null || request.password().trim().isEmpty()) {
            log.warn("Login failed: Password is required");
            throw new IllegalArgumentException("Password is required");
        }

        try {
            // Authenticate user and generate token
            String token = authService.login(request.username(), request.password());

            // Get user details to extract roles
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.username()));

            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            // Return token + username + roles
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("roles", roles);

            log.info("Login successful for user: {}", request.username());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user '{}': Invalid credentials", request.username());
            throw e; // GlobalExceptionHandler will convert to 401
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid UserRegistrationRequest request) {
        log.info("Registration request for username: {}", request.getUsername());

        // Additional validation
        if (request.getUsername() == null || request.getUsername().trim().length() < 3) {
            log.error("Registration failed: Username must be at least 3 characters");
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            log.error("Registration failed: Password must be at least 6 characters");
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        try {
            User user = authService.registerUserWithRoleIds(
                    request.getUsername(),
                    request.getPassword(),
                    request.getRoleIds()
            );

            UserResponse response = new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
            );

            log.info("User '{}' registered successfully", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Registration failed for username '{}': {}", request.getUsername(), e.getMessage());
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Get current user failed: No authentication");
            throw new BadCredentialsException("Not authenticated");
        }

        String username = authentication.getName();
        log.info("Get current user request for: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User '{}' not found", username);
                    return new ResourceNotFoundException("User", username);
                });

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody @Valid ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Change password failed: No authentication");
            throw new BadCredentialsException("Not authenticated");
        }

        String username = userDetails.getUsername();
        log.info("Change password request for user: {}", username);

        try {
            // Validate request
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Current password is required");
            }

            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters long");
            }

            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                throw new IllegalArgumentException("New password must be different from current password");
            }

            // Get current user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("User '{}' not found", username);
                        return new ResourceNotFoundException("User", username);
                    });

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Change password failed for '{}': Current password incorrect", username);
                throw new BadCredentialsException("Current password is incorrect");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", username);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));

        } catch (BadCredentialsException e) {
            log.warn("Change password failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Change password validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Change password failed for '{}': {}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }
}