package com.devcollab.service.system;

import com.devcollab.domain.Role;
import com.devcollab.domain.User;
import com.devcollab.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-exp-minutes}")
    private long jwtExpMinutes;

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ✅ Tạo Access Token có kèm roles từ DB
    public String generateAccessToken(String email) {
        User user = userRepository.findByEmailFetchRoles(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList()));

        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtExpMinutes * 60 * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Refresh token: chỉ chứa email
    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ✅ Trích xuất roles từ token
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?>) {
                return ((List<?>) rolesObj).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.out.println("[JwtService] No roles in token: " + e.getMessage());
        }
        return List.of("ROLE_MEMBER"); // fallback
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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
}
