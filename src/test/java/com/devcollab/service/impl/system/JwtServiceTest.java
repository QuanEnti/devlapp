package com.devcollab.service.impl.system;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.devcollab.service.system.JwtService;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        // Secret >= 32 bytes cho HS256
        String secret = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCD";
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpMinutes", 15L);
    }

    // J01: generateToken - hợp lệ
    @Test
    void generateToken_returnsValidJwtString() {
        String token = jwtService.generateToken("a@gmail.com");
        assertNotNull(token);
        assertTrue(token.split("\\.").length >= 3);
    }

    // J02: extractEmail - token hợp lệ
    @Test
    void extractEmail_validToken_returnsSubject() {
        String token = jwtService.generateToken("a@gmail.com");
        String email = jwtService.extractEmail(token);
        assertEquals("a@gmail.com", email);
    }

    // J03: extractEmail - token hỏng
    @Test
    void extractEmail_malformedToken_throwsJwtException() {
        String token = jwtService.generateToken("a@gmail.com");
        String tampered = token.substring(0, token.length() - 2); // cắt hỏng

        assertThrows(JwtException.class, () -> jwtService.extractEmail(tampered));
    }

    // J04: isValid - hợp lệ
    @Test
    void isValid_validToken_returnsTrue() {
        String token = jwtService.generateToken("a@gmail.com");
        assertTrue(jwtService.isValid(token));
    }

    // J05: isValid - sai chữ ký
    @Test
    void isValid_invalidSignature_returnsFalse() {
        String token = jwtService.generateToken("a@gmail.com");
        // Thay đổi 1 ký tự ở payload để làm hỏng chữ ký
        String[] parts = token.split("\\.");
        String corrupted = parts[0] + "." + parts[1] + "X." + parts[2];
        assertFalse(jwtService.isValid(corrupted));
    }
}
