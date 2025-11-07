package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.domain.UserSettings;
import com.devcollab.service.system.AuthService;
import com.devcollab.service.system.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings/notifications")
public class NotificationSettingsRestController {

    private final AuthService authService;
    private final UserSettingsService userSettingsService;

    /** ✅ Bật / tắt email notification (toggle nhanh trong popup) */
    @PostMapping("/email-toggle")
    @Transactional
    public ResponseEntity<?> toggleEmail(@RequestBody Map<String, Boolean> body, Authentication auth) {
        User user = authService.getCurrentUserEntity(auth);
        boolean enabled = body.getOrDefault("enabled", true);

        UserSettings settings = userSettingsService.getOrDefault(user);
        settings.setEmailEnabled(enabled);
        userSettingsService.save(settings);

        log.info("📩 Email notifications {} for {}", enabled ? "ENABLED" : "DISABLED", user.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok", "emailEnabled", enabled));
    }

    /** ✅ Lấy trạng thái hiện tại (dành cho popup hoặc trang settings) */
    @GetMapping("/me")
    public ResponseEntity<?> getSettings(Authentication auth) {
        User user = authService.getCurrentUserEntity(auth);
        UserSettings us = userSettingsService.getOrDefault(user);

        return ResponseEntity.ok(Map.of(
                "emailEnabled", us.isEmailEnabled(),
                "emailHighImmediate", us.isEmailHighImmediate(),
                "emailDigestEnabled", us.isEmailDigestEnabled(),
                "emailDigestEveryHours", us.getEmailDigestEveryHours()));
    }

    /** ✅ Cập nhật toàn bộ cài đặt thông báo (form chính) */
    @PostMapping("/update")
    @Transactional
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = authService.getCurrentUserEntity(auth);
        UserSettings us = userSettingsService.getOrDefault(user);

        boolean emailEnabled = (boolean) body.getOrDefault("emailEnabled", false);
        boolean emailHighImmediate = (boolean) body.getOrDefault("emailHighImmediate", false);
        boolean emailDigestEnabled = (boolean) body.getOrDefault("emailDigestEnabled", false);
        int emailDigestEveryHours = ((Number) body.getOrDefault("emailDigestEveryHours", 2)).intValue();

        if (emailDigestEveryHours != 2 && emailDigestEveryHours != 4 && emailDigestEveryHours != 6) {
            emailDigestEveryHours = 2;
        }

        us.setEmailEnabled(emailEnabled);
        us.setEmailHighImmediate(emailHighImmediate);
        us.setEmailDigestEnabled(emailDigestEnabled);
        us.setEmailDigestEveryHours(emailDigestEveryHours);

        userSettingsService.save(us);

        log.info("✅ [NotificationSettings] Updated via API for {} → enabled={}, digest={}h",
                user.getEmail(), emailEnabled, emailDigestEveryHours);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Settings updated successfully",
                "settings", us));
    }
}
