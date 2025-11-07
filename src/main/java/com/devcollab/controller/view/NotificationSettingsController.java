package com.devcollab.controller.view;

import com.devcollab.domain.User;
import com.devcollab.domain.UserSettings;
import com.devcollab.service.system.AuthService;
import com.devcollab.service.system.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/settings/notifications")
public class NotificationSettingsController {

    private final AuthService authService;
    private final UserSettingsService userSettingsService;

    @GetMapping
    public String view(Model model, Authentication auth) {
        // Kiểm tra nếu Authentication là null
        if (auth == null || auth.getPrincipal() == null) {
            log.error("❌ User not authenticated.");
            return "redirect:/login"; // Chuyển hướng đến trang đăng nhập nếu chưa đăng nhập
        }

        // Lấy thông tin người dùng từ AuthService
        User user = authService.getCurrentUserEntity(auth);
        if (user == null) {
            log.error("❌ Failed to get current user.");
            return "redirect:/login"; // Chuyển hướng đến trang đăng nhập nếu không tìm thấy người dùng
        }

        // Lấy cài đặt người dùng
        UserSettings settings = userSettingsService.getOrDefault(user);
        if (settings == null) {
            log.warn("⚠️ User settings not found, using default settings.");
            settings = new UserSettings(); // Trả về cài đặt mặc định nếu không có cài đặt người dùng
        }

        model.addAttribute("settings", settings);
        return "settings/notification-settings";
    }
}
