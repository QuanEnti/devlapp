package com.devcollab.service.impl.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.devcollab.service.system.OtpService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)

class OtpServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private OtpService otpService;

    @BeforeEach
    void setup() {
        otpService = new OtpService(redisTemplate);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "otpTtlSeconds", 300L);
        ReflectionTestUtils.setField(otpService, "cooldownSeconds", 15L);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // O01: generateOtp() - độ dài hợp lệ
    @Test
    void generateOtp_length6_numeric() {
        String otp = otpService.generateOtp();
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    // O02: generateOtp() - otpLength = 0 (boundary)
    @Test
    void generateOtp_lengthZero_returnsEmpty() {
        ReflectionTestUtils.setField(otpService, "otpLength", 0);
        String otp = otpService.generateOtp();
        assertEquals(0, otp.length());
    }

    // O03: storeOtp() - lưu thành công (verify set 2 lần)
    @Test
    void storeOtp_success_setsOtpAndCooldown() {
        String email = "bob@example.com";
        String otp = "123456";

        otpService.storeOtp(email, otp);

        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOps).set(eq("otp:" + email), eq(otp), any(Duration.class));
        verify(valueOps).set(eq("otp:cooldown:" + email), eq("1"), any(Duration.class));
    }

    // O04: storeOtp() - email null (negative)
    @Test
    void storeOtp_emailNull_throwsNpe() {
        assertThrows(NullPointerException.class, () -> otpService.storeOtp(null, "111111"));
        verify(redisTemplate, never()).opsForValue();
    }

    // O05: isInCooldown() - có key
    @Test
    void isInCooldown_keyExists_returnsTrue() {
        String email = "bob@example.com";
        when(redisTemplate.hasKey("otp:cooldown:" + email)).thenReturn(Boolean.TRUE);

        boolean result = otpService.isInCooldown(email, 15);
        assertTrue(result);
        verify(redisTemplate).hasKey("otp:cooldown:" + email);
    }

    // O06: isInCooldown() - không có key
    @Test
    void isInCooldown_keyNotExists_returnsFalse() {
        String email = "bob@example.com";
        when(redisTemplate.hasKey("otp:cooldown:" + email)).thenReturn(Boolean.FALSE);

        boolean result = otpService.isInCooldown(email, 15);
        assertFalse(result);
        verify(redisTemplate).hasKey("otp:cooldown:" + email);
    }

    // O07: verifyOtp() - đúng OTP
    @Test
    void verifyOtp_correct_returnsTrueAndDelete() {
        String email = "eve@example.com";
        when(valueOps.get("otp:" + email)).thenReturn("654321");

        boolean ok = otpService.verifyOtp(email, "654321");

        assertTrue(ok);
        verify(valueOps).get("otp:" + email);
        verify(redisTemplate).delete("otp:" + email);
    }

    // O08: verifyOtp() - sai OTP
    @Test
    void verifyOtp_wrong_returnsFalse() {
        String email = "eve@example.com";
        when(valueOps.get("otp:" + email)).thenReturn("654321");

        boolean ok = otpService.verifyOtp(email, "000000");

        assertFalse(ok);
        verify(valueOps).get("otp:" + email);
        verify(redisTemplate, never()).delete(anyString());
    }

    // O09: verifyOtp() - OTP hết hạn (null)
    @Test
    void verifyOtp_expired_returnsFalse() {
        String email = "eve@example.com";
        when(valueOps.get("otp:" + email)).thenReturn(null);

        boolean ok = otpService.verifyOtp(email, "654321");

        assertFalse(ok);
        verify(valueOps).get("otp:" + email);
        verify(redisTemplate, never()).delete(anyString());
    }
}
