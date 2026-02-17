package com.ecommerce.auth.util;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationTime;

    public JwtUtil(
            @Value("${app.jwt.secret:my-super-secret-key-change-this-in-prod}") String secret,
            @Value("${app.jwt.expiration:3600000}") long expirationTime
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    /**
     * Generate JWT token
     */
    public String generateToken(User user) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            getAllClaims(token);
            return true;
        } catch (Exception ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extract username
     */
    public String extractUsername(String token) {
        return getAllClaims(token).get("username", String.class);
    }

    /**
     * Extract userId
     */
    public Long extractUserId(String token) {
        String subject = getAllClaims(token).getSubject();
        return Long.parseLong(subject);
    }

    /**
     * Extract role
     */
    public UserRole extractRole(String token) {
        String role = getAllClaims(token).get("role", String.class);
        return UserRole.valueOf(role);
    }

    /**
     * Internal claims extractor
     */
    private Claims getAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}