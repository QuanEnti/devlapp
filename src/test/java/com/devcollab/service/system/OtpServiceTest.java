package com.devcollab.service.system;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.quality.Strictness;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // ✅ Cho phép stub không dùng
class OtpServiceTest {

    @Mock(lenient = true)
    private RedisTemplate<String, String> redis;

    @Mock(lenient = true)
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void init() {
        // ✅ Stub lenient để tránh lỗi "Unnecessary stubbing"
        lenient().when(redis.opsForValue()).thenReturn(valueOps);

        // ✅ Inject field cấu hình
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "otpTtlSeconds", 300L);
        ReflectionTestUtils.setField(otpService, "cooldownSeconds", 15L);
    }

    // ✅ TC09: Sinh OTP hợp lệ
    @Test
    void TC09_generateOtp_shouldBe6Digits() {
        String otp = otpService.generateOtp();
        assertTrue(otp.matches("\\d{6}"));
    }

    // ✅ TC10: Lưu OTP và cooldown vào Redis
    @Test
    void TC10_storeOtp_shouldCallRedis() {
        otpService.storeOtp("a@mail.com", "111111");

        InOrder order = inOrder(valueOps);
        order.verify(valueOps).set(eq("otp:a@mail.com"), eq("111111"), eq(Duration.ofSeconds(300)));
        order.verify(valueOps).set(eq("otp:cooldown:a@mail.com"), eq("1"),
                eq(Duration.ofSeconds(15)));
    }

    // ✅ TC11: Cooldown tồn tại → true
    @Test
    void TC11_isInCooldown_true() {
        when(redis.hasKey("otp:cooldown:x@gmail.com")).thenReturn(true);
        assertTrue(otpService.isInCooldown("x@gmail.com", 15));
    }

    // ✅ TC12: Cooldown không tồn tại → false
    @Test
    void TC12_isInCooldown_false() {
        when(redis.hasKey("otp:cooldown:x@gmail.com")).thenReturn(false);
        assertFalse(otpService.isInCooldown("x@gmail.com", 15));
    }

    // ✅ TC13: OTP đúng → verify delete
    @Test
    void TC13_verifyOtp_success() {
        when(valueOps.get("otp:test@mail.com")).thenReturn("123456");
        assertTrue(otpService.verifyOtp("test@mail.com", "123456"));
        verify(redis).delete("otp:test@mail.com");
    }

    // ✅ TC14: OTP sai → không xóa key
    @Test
    void TC14_verifyOtp_wrong() {
        when(valueOps.get("otp:test@mail.com")).thenReturn("654321");
        assertFalse(otpService.verifyOtp("test@mail.com", "000000"));
        verify(redis, never()).delete(anyString());
    }

    // ✅ TC15: OTP null hoặc hết hạn
    @Test
    void TC15_verifyOtp_null() {
        when(valueOps.get("otp:test@mail.com")).thenReturn(null);
        assertFalse(otpService.verifyOtp("test@mail.com", "123"));
    }

    // ✅ TC16: buildOtpKey normalize
    @Test
    void TC16_buildOtpKey_shouldNormalize() {
        String key = (String) ReflectionTestUtils.invokeMethod(otpService, "buildOtpKey",
                "MyEmail@gmail.com");
        assertEquals("otp:myemail@gmail.com", key);
    }

    // ✅ TC17: buildCooldownKey normalize
    @Test
    void TC17_buildCooldownKey_shouldNormalize() {
        String key = (String) ReflectionTestUtils.invokeMethod(otpService, "buildCooldownKey",
                "MyEmail@gmail.com");
        assertEquals("otp:cooldown:myemail@gmail.com", key);
    }

    // ✅ TC18: TTL = 0 → Expire ngay lập tức
    @Test
    void TC18_TTLZero_shouldExpireImmediately() {
        ReflectionTestUtils.setField(otpService, "otpTtlSeconds", 0L);
        otpService.storeOtp("zero@mail.com", "000000");
        verify(valueOps).set(eq("otp:zero@mail.com"), eq("000000"), eq(Duration.ofSeconds(0)));
    }

    // ✅ TC19: otpLength = 0 → trả chuỗi rỗng
    @Test
    void TC19_otpLengthZero_shouldReturnEmpty() {
        ReflectionTestUtils.setField(otpService, "otpLength", 0);
        String otp = otpService.generateOtp();
        assertEquals("", otp);
    }
}
