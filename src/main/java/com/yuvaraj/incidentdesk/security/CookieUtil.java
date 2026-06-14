package com.yuvaraj.incidentdesk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String AUTH_COOKIE = "token";

    private final boolean secure;
    private final long days;

    public CookieUtil(@Value("${app.cookie-secure}") boolean secure,
                      @Value("${app.jwt.expires-in-days}") long days) {
        this.secure = secure;
        this.days = days;
    }

    public ResponseCookie build(String token) {
        return ResponseCookie.from(AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofDays(days))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
