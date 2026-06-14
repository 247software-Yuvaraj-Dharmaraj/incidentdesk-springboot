package com.yuvaraj.incidentdesk.service;

import com.yuvaraj.incidentdesk.domain.User;
import com.yuvaraj.incidentdesk.dto.AuthDtos.LoginRequest;
import com.yuvaraj.incidentdesk.dto.AuthDtos.PreferencesRequest;
import com.yuvaraj.incidentdesk.dto.AuthDtos.SignupRequest;
import com.yuvaraj.incidentdesk.exception.ApiException;
import com.yuvaraj.incidentdesk.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(SignupRequest input) {
        users.findByEmail(input.email()).ifPresent(u -> {
            throw ApiException.conflict("An account with this email already exists");
        });
        User user = new User();
        user.setEmail(input.email());
        user.setPasswordHash(passwordEncoder.encode(input.password()));
        user.setFullName(input.fullName());
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest input) {
        User user = users.findByEmail(input.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!passwordEncoder.matches(input.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getById(String id) {
        return users.findById(id).orElse(null);
    }

    @Transactional
    public User updatePreferences(String id, PreferencesRequest prefs) {
        User user = users.findById(id)
                .orElseThrow(() -> ApiException.unauthorized("Session user no longer exists"));
        if (prefs.theme() != null) {
            user.setTheme(prefs.theme());
        }
        if (prefs.density() != null) {
            user.setDensity(prefs.density());
        }
        return users.save(user);
    }
}
