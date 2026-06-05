package com.example.bai2;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;


public class InsecureTokenService {
    @Value("${JWT_SECRET_KEY}")
    private String HARDCODED_SECRET_KEY;
    @Value("${JWT_EXPIRATION_DAYS}")
    private Long ACCESS_TOKEN_EXPIRATION;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(HARDCODED_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            System.out.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }
}