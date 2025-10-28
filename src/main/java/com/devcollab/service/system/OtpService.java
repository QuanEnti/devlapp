package com.devcollab.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;
    
    @Value("${app.otp.ttl-seconds:300}") 
    private long otpTtlSeconds;

    @Value("${app.otp.cooldown-seconds:15}") 
    private long cooldownSeconds;

    public String generateOtp() {
        StringBuilder otp = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    public void storeOtp(String email, String otp) {
        String otpKey = buildOtpKey(email);
        redisTemplate.opsForValue().set(otpKey, otp, Duration.ofSeconds(otpTtlSeconds));

        String cooldownKey = buildCooldownKey(email);
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds));

        System.out.printf("[OtpService] Stored OTP %s for %s (TTL: %ds, Cooldown: %ds)%n",
                otp, email, otpTtlSeconds, cooldownSeconds);
    }

    public boolean isInCooldown(String email, int cooldownSeconds) {
        String cooldownKey = buildCooldownKey(email);
        Boolean exists = redisTemplate.hasKey(cooldownKey);
        return Boolean.TRUE.equals(exists);
    }
    public boolean verifyOtp(String email, String inputOtp) {
        String otpKey = buildOtpKey(email);
        System.out.println("[DEBUG] OTP Key check: " + otpKey);
        Boolean exists = redisTemplate.hasKey(otpKey);
        System.out.println("[DEBUG] Exists in Redis: " + exists);

        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            System.out.printf("⚠️ [OtpService] No OTP found or expired for %s%n", email);
            return false;
        }

        if (!storedOtp.equals(inputOtp)) {
            System.out.printf(" [OtpService] Invalid OTP for %s (expected %s, got %s)%n",
                    email, storedOtp, inputOtp);
            return false;
        }

        redisTemplate.delete(otpKey);
        System.out.printf("[OtpService] OTP verified successfully for %s%n", email);
        return true;
    }

    private String buildOtpKey(String email) {
        return "otp:" + email.trim().toLowerCase();
    }

    private String buildCooldownKey(String email) {
        return "otp:cooldown:" + email.trim().toLowerCase();
    }
}
