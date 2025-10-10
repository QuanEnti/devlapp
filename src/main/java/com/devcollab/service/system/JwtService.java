package com.devcollab.service.system;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-exp-minutes}")
    private long jwtExpMinutes;

    @SuppressWarnings("deprecation")
    public String generateToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpMinutes * 60 * 1000);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }

    @SuppressWarnings("deprecation")
    public String extractEmail(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    @SuppressWarnings("deprecation")
    public boolean isValid(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret.getBytes()).parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
