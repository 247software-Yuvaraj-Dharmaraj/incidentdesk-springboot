package com.yuvaraj.incidentdesk.web;

import com.yuvaraj.incidentdesk.domain.User;
import com.yuvaraj.incidentdesk.dto.AuthDtos.LoginRequest;
import com.yuvaraj.incidentdesk.dto.AuthDtos.SignupRequest;
import com.yuvaraj.incidentdesk.dto.AuthDtos.UserResponse;
import com.yuvaraj.incidentdesk.exception.ApiException;
import com.yuvaraj.incidentdesk.ratelimit.RateLimiter;
import com.yuvaraj.incidentdesk.security.AppUser;
import com.yuvaraj.incidentdesk.security.CookieUtil;
import com.yuvaraj.incidentdesk.security.JwtService;
import com.yuvaraj.incidentdesk.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService authService, JwtService jwtService, CookieUtil cookieUtil, RateLimiter rateLimiter) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest http) {
        rateLimiter.check("auth:" + http.getRemoteAddr(), 20, 60);
        User user = authService.register(request);
        return withCookie(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        rateLimiter.check("auth:" + http.getRemoteAddr(), 20, 60);
        User user = authService.authenticate(request);
        return withCookie(user, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AppUser principal) {
        User user = authService.getById(principal.id());
        if (user == null) {
            throw ApiException.unauthorized("Session user no longer exists");
        }
        return Map.of("user", UserResponse.from(user));
    }

    private ResponseEntity<Map<String, Object>> withCookie(User user, HttpStatus status) {
        String token = jwtService.generate(user.getId(), user.getRole());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieUtil.build(token).toString())
                .body(Map.of("user", UserResponse.from(user)));
    }
}
