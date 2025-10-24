package com.devcollab.service.impl.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.devcollab.service.system.OtpService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Thiết lập giá trị @Value
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "otpTtlSeconds", 300L);
        ReflectionTestUtils.setField(otpService, "cooldownSeconds", 15L);
    }

    @Test // ✅ TC15
    void generateOtp_ShouldReturnNumericStringWithCorrectLength() {
        String otp = otpService.generateOtp();
        assertNotNull(otp);
        assertEquals(6, otp.length(), "OTP phải có độ dài = otpLength");
        assertTrue(otp.matches("\\d{6}"), "OTP chỉ gồm các ký tự số");
    }

    @Test // ✅ TC16
    void storeOtp_ShouldStoreOtpAndCooldownInRedis() {
        String email = "user@mail.com";
        String otp = "123456";

        otpService.storeOtp(email, otp);

        verify(valueOps, times(1))
                .set(eq("otp:user@mail.com"), eq("123456"), eq(Duration.ofSeconds(300L)));
        verify(valueOps, times(1))
                .set(eq("otp:cooldown:user@mail.com"), eq("1"), eq(Duration.ofSeconds(15L)));
    }

    @Test // ✅ TC17
    void isInCooldown_ShouldReturnTrue_WhenKeyExists() {
        when(redisTemplate.hasKey("otp:cooldown:abc@gmail.com")).thenReturn(true);
        boolean result = otpService.isInCooldown("abc@gmail.com", 15);
        assertTrue(result);
    }

    @Test // ✅ TC18
    void isInCooldown_ShouldReturnFalse_WhenKeyMissing() {
        when(redisTemplate.hasKey("otp:cooldown:abc@gmail.com")).thenReturn(false);
        boolean result = otpService.isInCooldown("abc@gmail.com", 15);
        assertFalse(result);
    }

    @Test // ✅ TC19
    void verifyOtp_ShouldReturnTrue_WhenOtpMatches() {
        when(valueOps.get("otp:test@mail.com")).thenReturn("999999");
        boolean result = otpService.verifyOtp("test@mail.com", "999999");
        assertTrue(result, "OTP hợp lệ phải trả về true");
        verify(redisTemplate).delete("otp:test@mail.com");
    }

    @Test // ✅ TC20
    void verifyOtp_ShouldReturnFalse_WhenOtpDoesNotMatch() {
        when(valueOps.get("otp:test@mail.com")).thenReturn("000000");
        boolean result = otpService.verifyOtp("test@mail.com", "111111");
        assertFalse(result, "OTP sai phải trả về false");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test // ✅ TC21
    void verifyOtp_ShouldReturnFalse_WhenOtpExpiredOrNull() {
        when(valueOps.get("otp:test@mail.com")).thenReturn(null);
        boolean result = otpService.verifyOtp("test@mail.com", "123456");
        assertFalse(result, "OTP null hoặc hết hạn phải trả về false");
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test // ✅ TC22
    void verifyOtp_ShouldDeleteKey_WhenOtpMatches() {
        when(valueOps.get("otp:user@mail.com")).thenReturn("777777");
        otpService.verifyOtp("user@mail.com", "777777");
        verify(redisTemplate).delete("otp:user@mail.com");
    }

    @Test // ✅ TC23
    void buildKeys_ShouldReturnFormattedStrings() {
        // Dùng Reflection để gọi hàm private
        String otpKey = (String) ReflectionTestUtils.invokeMethod(otpService, "buildOtpKey", "MyEmail@gmail.com");
        String cooldownKey = (String) ReflectionTestUtils.invokeMethod(otpService, "buildCooldownKey",
                "MyEmail@gmail.com");

        assertEquals("otp:myemail@gmail.com", otpKey);
        assertEquals("otp:cooldown:myemail@gmail.com", cooldownKey);
    }
}
