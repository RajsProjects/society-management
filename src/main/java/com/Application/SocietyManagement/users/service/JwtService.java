package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration}")
    private long EXPIRATION;

    private SecretKey signingKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void validateConfiguration() {
        if (SECRET != null && !SECRET.isBlank() && SECRET.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes (256 bits) long for HMAC-SHA algorithms.");
        }
        this.signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(this.signingKey).build();
    }

    private SecretKey getSigningKey() {
        return this.signingKey != null ? this.signingKey : Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .claim("platformRole", user.getPlatformRole() != null ? user.getPlatformRole().name() : null)
                .claim("status", user.getStatus() != null ? user.getStatus().name() : null)
                .claim("societyId", user.getSocietyId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractSocietyId(String token) {
        return getClaims(token).get("societyId", String.class);
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String extractPlatformRole(String token) {
        return getClaims(token).get("platformRole", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // expired or invalid token
        }
    }
    public Claims parseAndValidateClaims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            JwtParser parser = this.jwtParser != null ? this.jwtParser : Jwts.parser().verifyWith(getSigningKey()).build();
            return parser.parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValid(String token) {
        return parseAndValidateClaims(token) != null;
    }

    private Claims getClaims(String token) {
        JwtParser parser = this.jwtParser != null ? this.jwtParser : Jwts.parser().verifyWith(getSigningKey()).build();
        return parser.parseSignedClaims(token).getPayload();
    }
}