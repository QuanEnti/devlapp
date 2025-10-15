package com.devcollab.service.system;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-exp-minutes}")
    private long jwtExpMinutes;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpMinutes * 60 * 1000);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            System.out.println("[JwtService] Token invalid: " + e.getMessage());
            return false;
        }
    }

    public String generateAccessToken(String email) {
        return generateToken(email);
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
