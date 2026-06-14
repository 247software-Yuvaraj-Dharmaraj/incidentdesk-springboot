package com.yuvaraj.incidentdesk.dto;

import com.yuvaraj.incidentdesk.domain.Role;
import com.yuvaraj.incidentdesk.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
            @NotBlank @Size(min = 2, message = "Name must be at least 2 characters") String fullName) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record UserResponse(String id, String email, String fullName, Role role, String theme, String density, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getTheme(), u.getDensity(), u.getCreatedAt());
        }
    }

    public record PreferencesRequest(
            @Pattern(regexp = "light|dark", message = "theme must be light or dark") String theme,
            @Pattern(regexp = "comfortable|compact", message = "density must be comfortable or compact") String density) {
    }

    public record UserListItem(String id, String fullName, String email, Role role) {
        public static UserListItem from(User u) {
            return new UserListItem(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
        }
    }

    public record UserPreview(String id, String fullName, String email) {
        public static UserPreview from(User u) {
            return u == null ? null : new UserPreview(u.getId(), u.getFullName(), u.getEmail());
        }
    }
}
