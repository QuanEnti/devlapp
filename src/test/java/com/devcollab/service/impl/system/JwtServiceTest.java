package com.devcollab.service.impl.system;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.devcollab.service.system.JwtService;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService();
        // Thiết lập giá trị cho @Value bằng ReflectionTestUtils
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "0123456789012345678901234567890123456789012345678901234567890123"); // 64 ký tự (HMAC key)
        ReflectionTestUtils.setField(jwtService, "jwtExpMinutes", 1L); // 1 phút
    }

    @Test // ✅ TC09
    void generateToken_ShouldReturnValidJWT() {
        String token = jwtService.generateToken("user@example.com");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT phải có 3 phần header.payload.signature");
    }

    @Test // ✅ TC10
    void extractEmail_ShouldReturnCorrectEmail() {
        String token = jwtService.generateToken("abc@gmail.com");
        String email = jwtService.extractEmail(token);
        assertEquals("abc@gmail.com", email);
    }

    @Test // ✅ TC11
    void isValid_ShouldReturnTrue_WhenTokenIsValid() {
        String token = jwtService.generateToken("valid@gmail.com");
        assertTrue(jwtService.isValid(token));
    }

    @Test // ✅ TC12
    void isValid_ShouldReturnFalse_WhenSignatureInvalid() {
        String token = jwtService.generateToken("hacker@gmail.com");
        // Làm token sai signature
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtService.isValid(tampered));
    }

    @Test // ✅ TC13
    void isValid_ShouldReturnFalse_WhenExpired() throws InterruptedException {
        // Giảm thời gian sống xuống 1 giây
        ReflectionTestUtils.setField(jwtService, "jwtExpMinutes", 0L);
        String token = jwtService.generateToken("expire@gmail.com");

        // Đợi token hết hạn
        Thread.sleep(1500);

        assertFalse(jwtService.isValid(token));
    }

    @Test // ✅ TC14
    void generateRefreshToken_ShouldHave7DaysTTL() {
        long now = System.currentTimeMillis();
        String refreshToken = jwtService.generateRefreshToken("refresh@gmail.com");

        // Giải mã để lấy thời gian hết hạn
        Date exp = io.jsonwebtoken.Jwts.parserBuilder()
                .setSigningKey(ReflectionTestUtils.getField(jwtService, "jwtSecret").toString().getBytes())
                .build()
                .parseClaimsJws(refreshToken)
                .getBody()
                .getExpiration();

        long diffMs = exp.getTime() - now;
        long diffDays = diffMs / (1000 * 60 * 60 * 24);
        assertTrue(diffDays >= 6 && diffDays <= 8, "Refresh token phải có TTL khoảng 7 ngày");
    }
}
