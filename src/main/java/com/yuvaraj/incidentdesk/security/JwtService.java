package com.yuvaraj.incidentdesk.security;

import com.yuvaraj.incidentdesk.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiresMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expires-in-days}") long expiresInDays) {
        // Derive a fixed 256-bit key from the configured secret so any secret length works with HS256.
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.expiresMillis = expiresInDays * 24L * 60L * 60L * 1000L;
    }

    public String generate(String userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiresMillis)))
                .signWith(key)
                .compact();
    }

    public AppUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AppUser(claims.getSubject(), Role.valueOf(claims.get("role", String.class)));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
