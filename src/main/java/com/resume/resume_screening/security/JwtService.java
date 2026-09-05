package com.resume.resume_screening.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "resumeScreeningSecretKeyForJwtAuthentication123456";

    private static final long EXPIRATION_TIME =
        1000L * 60 * 60 * 4;
    private final SecretKey key;

    public JwtService() {
        this.key = Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8)
        );
    }

 public String generateToken(String email, String role) {

    return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(
                    new Date(System.currentTimeMillis()
                            + EXPIRATION_TIME)
            )
            .signWith(key)
            .compact();
}

    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token, String email) {

        try {
            String tokenEmail = extractEmail(token);

            return tokenEmail.equals(email);

        } catch (Exception e) {
            return false;
        }
    }
    public String extractRole(String token) {

    return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("role", String.class);
}
}